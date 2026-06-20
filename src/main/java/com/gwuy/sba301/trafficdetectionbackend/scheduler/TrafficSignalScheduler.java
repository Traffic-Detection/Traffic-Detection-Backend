package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.atcs.algorithm.TrafficAlgorithmService;
import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.SignalHistoryService;
import com.gwuy.sba301.trafficdetectionbackend.service.WebSocketService;
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
    private final SignalHistoryService signalHistoryService;
    private final WebSocketService webSocketService;

    @Scheduled(fixedRate = 5000)
    public void runTrafficEngine() {
        long startTime = System.currentTimeMillis();
        log.info("[Scheduler] Started");

        try {
            // 1. Read DB: Get intersections in AI_AUTO mode
            List<Intersection> intersections = intersectionRepository.findByOperatingMode(OperatingMode.AI_AUTO);
            
            for (Intersection intersection : intersections) {
                // 2. AI Algorithm & Processing
                Map<String, Object> result = trafficAlgorithmService.calculateAdaptiveSignal(intersection);
                
                if (result.isEmpty()) continue;

                @SuppressWarnings("unchecked")
                List<SignalHistory> histories = (List<SignalHistory>) result.get("histories");
                @SuppressWarnings("unchecked")
                List<SignalMessage> messages = (List<SignalMessage>) result.get("messages");

                // 3. Save DB: SignalHistory
                signalHistoryService.saveAll(histories);

                // 4. Send WebSocket realtime
                webSocketService.sendSignalUpdates(messages);
            }

        } catch (Exception e) {
            log.error("[Scheduler] Error in TrafficSignalScheduler: ", e);
        }

        long endTime = System.currentTimeMillis();
        log.info("[Scheduler] Finished in {} ms", (endTime - startTime));
    }
}
