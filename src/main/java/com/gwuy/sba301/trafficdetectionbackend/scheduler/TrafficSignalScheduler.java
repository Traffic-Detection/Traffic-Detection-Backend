package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalHistoryService;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.WebSocketServiceImpl;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficSignalScheduler {

    private final IntersectionRepository intersectionRepository;
    private final TrafficAlgorithmService trafficAlgorithmServiceImpl;
    private final SignalHistoryService signalHistoryService;
    private final WebSocketServiceImpl webSocketServiceImpl;
    private final ManualSignalService manualSignalService;

    @Scheduled(fixedRate = 5000)
    public void runTrafficEngine() {
        long startTime = System.currentTimeMillis();
        log.debug("[Scheduler] Started");

        int processedAi = 0;
        int processedManual = 0;

        try {
            List<Intersection> intersections = intersectionRepository.findAllWithLanes();

            if (intersections.isEmpty()) {
                log.debug("[Scheduler] No intersections with lanes found");
                return;
            }

            for (Intersection intersection : intersections) {
                OperatingMode mode = intersection.getOperatingMode();

                if (mode == OperatingMode.AI) {
                    // === NHÁNH AI ===
                    Map<String, Object> result = trafficAlgorithmServiceImpl.calculateAdaptiveSignal(intersection);
                    if (result.isEmpty()) continue;

                    @SuppressWarnings("unchecked")
                    List<SignalHistory> histories = (List<SignalHistory>) result.get("histories");
                    @SuppressWarnings("unchecked")
                    List<SignalMessage> messages = (List<SignalMessage>) result.get("messages");

                    signalHistoryService.saveAll(histories);
                    webSocketServiceImpl.sendSignalUpdates(messages);

                    processedAi++;

                } else if (mode == OperatingMode.MANUAL) {
                    // === NHÁNH MANUAL ===
                    List<SignalMessage> manualMessages = manualSignalService.getFixedSignals(intersection);

                    if (!manualMessages.isEmpty()) {
                        // Theo thiết kế: KHÔNG lưu signal_history cho chế độ MANUAL
                        webSocketServiceImpl.sendSignalUpdates(manualMessages);
                        processedManual++;
                    }
                }
            }

        } catch (Exception e) {
            log.error("[Scheduler] Error in TrafficSignalScheduler: ", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[Scheduler] Finished - AI: {}, MANUAL: {}, duration: {}ms",
                processedAi, processedManual, duration);
    }
}