package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.atcs.algorithm.TrafficAlgorithmService;
import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ISignalHistoryService;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.OperatingModeGuard;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.WebWebSocketService;
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
    private final TrafficAlgorithmService trafficAlgorithmService;
    private final ISignalHistoryService signalHistoryService;
    private final WebWebSocketService webSocketService;
    private final OperatingModeGuard operatingModeGuard;

    @Scheduled(fixedRate = 5000)
    public void runTrafficEngine() {
        long startTime = System.currentTimeMillis();
        log.info("[Scheduler] Started");

        int processed = 0;
        int skipped = 0;

        try {
            List<Intersection> intersections = intersectionRepository.findAllWithLanes();

            if (intersections.isEmpty()) {
                log.info("[Scheduler] No intersections with lanes found");
                return;
            }

            log.info("[Scheduler] Found {} intersections", intersections.size());

            for (Intersection intersection : intersections) {

                if (!operatingModeGuard.isAiProcessingAllowed(intersection)) {
                    log.info("[Scheduler] Skip intersection {} ({}) - reason: {}",
                            intersection.getId(),
                            intersection.getName(),
                            intersection.getOperatingMode());
                    skipped++;
                    continue;
                }

                log.info("[Scheduler] Processing intersection {} ({})",
                        intersection.getId(),
                        intersection.getName());

                Map<String, Object> result = trafficAlgorithmService.calculateAdaptiveSignal(intersection);
                if (result.isEmpty()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<SignalHistory> histories = (List<SignalHistory>) result.get("histories");
                @SuppressWarnings("unchecked")
                List<SignalMessage> messages = (List<SignalMessage>) result.get("messages");

                signalHistoryService.saveAll(histories);
                webSocketService.sendSignalUpdates(messages);

                processed++;
            }

        } catch (Exception e) {
            log.error("[Scheduler] Error in TrafficSignalScheduler: ", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[Scheduler] Finished - processed: {}, skipped: {}, duration: {}ms",
                processed, skipped, duration);
    }
}
