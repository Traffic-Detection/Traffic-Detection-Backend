package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.ModeSwitchManager;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalHistoryService;
import com.gwuy.sba301.trafficdetectionbackend.service.impls.WebSocketServiceImpl;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficSignalScheduler {

    private final IntersectionRepository intersectionRepository;
    private final TrafficAlgorithmService trafficAlgorithmServiceImpl;
    private final SignalHistoryService signalHistoryService;
    private final WebSocketServiceImpl webSocketServiceImpl;
    private final ManualSignalService manualSignalService;
    private final ModeSwitchManager modeSwitchManager;

    private final Map<Long, Instant> aiCooldownMap = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 1000) // Quét mỗi giây, nhưng bị khóa bởi Cooldown
    @Transactional
    public void runTrafficEngine() {
        try {
            List<Intersection> intersections = intersectionRepository.findAllWithLanes();
            if (intersections.isEmpty()) return;

            Instant now = Instant.now();

            for (Intersection intersection : intersections) {
                OperatingMode mode = intersection.getOperatingMode();

                if (mode == OperatingMode.AI) {
                    if (!modeSwitchManager.isAiReady(intersection.getId())) {
                        continue;
                    }

                    Instant nextAllowedRun = aiCooldownMap.getOrDefault(intersection.getId(), Instant.MIN);

                    // NẾU ĐANG TRONG CHU KỲ 80S -> BỎ QUA. Để Frontend tự đếm lùi cho mượt!
                    if (now.isBefore(nextAllowedRun)) {
                        continue;
                    }

                    // HẾT 80S -> GỌI AI ĐỌC DATA VÀ CHỐT CHU KỲ MỚI
                    Map<String, Object> result = trafficAlgorithmServiceImpl.calculateAdaptiveSignal(intersection);
                    if (result.isEmpty()) continue;

                    @SuppressWarnings("unchecked")
                    List<SignalHistory> histories = (List<SignalHistory>) result.get("histories");
                    @SuppressWarnings("unchecked")
                    List<SignalMessage> messages = (List<SignalMessage>) result.get("messages");

                    signalHistoryService.saveAll(histories);
                    webSocketServiceImpl.sendSignalUpdates(messages);

                    // TÍNH TỔNG CHU KỲ = Xanh + Đỏ (Bảo đảm luôn = 80s)
                    long totalCycle = messages.get(0).getGreenDuration() + messages.get(0).getRedDuration();

                    // Khóa mỏ Backend lại trong đúng 80s
                    aiCooldownMap.put(intersection.getId(), now.plusSeconds(totalCycle));

                    log.info("[Scheduler] Đã kích hoạt chu kỳ mới cho Giao lộ: {}. AI sẽ ngủ đông {}s.", intersection.getName(), totalCycle);

                } else if (mode == OperatingMode.MANUAL) {
                    List<SignalMessage> manualMessages = manualSignalService.getFixedSignals(intersection);
                    if (!manualMessages.isEmpty()) {
                        webSocketServiceImpl.sendSignalUpdates(manualMessages);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[Scheduler] Error in TrafficSignalScheduler: ", e);
        }
    }
}