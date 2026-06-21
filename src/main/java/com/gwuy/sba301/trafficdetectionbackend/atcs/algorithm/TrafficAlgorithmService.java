package com.gwuy.sba301.trafficdetectionbackend.atcs.algorithm;

import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.Lane;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.entity.TrafficLog;
import com.gwuy.sba301.trafficdetectionbackend.repository.LaneRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.TrafficLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficAlgorithmService {

    private final LaneRepository laneRepository;
    private final TrafficLogRepository trafficLogRepository;

    private static final int TOTAL_CYCLE = 80;

    public Map<String, Object> calculateAdaptiveSignal(Intersection intersection) {
        log.info("[AI] Processing intersection: {}", intersection.getName());
        
        List<Lane> lanes = laneRepository.findByIntersectionId(intersection.getId());
        if (lanes.isEmpty()) {
            log.warn("[AI] No lanes found for intersection: {}", intersection.getName());
            return Collections.emptyMap();
        }

        // 1. Get latest TrafficLog for each lane and calculate congestion per lane
        Map<Long, Double> laneCongestionMap = new HashMap<>();
        for (Lane lane : lanes) {
            double congestion = trafficLogRepository.findFirstByLaneIdOrderByRecordedAtDesc(lane.getId())
                    .map(TrafficLog::getCongestionLevel)
                    .orElse(0.0);
            laneCongestionMap.put(lane.getId(), congestion);
        }

        // 2. Identify pair of lanes with highest congestion
        // For each lane, the pair's congestion is (lane + opposing lane)
        Lane bestLane = null;
        double maxPairCongestion = -1;

        Set<Long> visited = new HashSet<>();

        for (Lane lane : lanes) {

            if (visited.contains(lane.getId())) {
                continue;
            }

            double laneCongestion = laneCongestionMap.getOrDefault(lane.getId(), 0.0);
            double opposingCongestion = 0.0;

            if (lane.getOpposingLane() != null) {
                opposingCongestion = laneCongestionMap.getOrDefault(
                        lane.getOpposingLane().getId(),
                        0.0
                );
                visited.add(lane.getOpposingLane().getId());
            }

            visited.add(lane.getId());

            double pairCongestion = laneCongestion + opposingCongestion;

            log.info(
                    "[AI] Pair {}({}) + {}({}) = {}",
                    lane.getDirectionName(),
                    laneCongestion,
                    lane.getOpposingLane() == null ? "-" : lane.getOpposingLane().getDirectionName(),
                    opposingCongestion,
                    pairCongestion
            );

            if (pairCongestion > maxPairCongestion) {
                maxPairCongestion = pairCongestion;
                bestLane = lane;
            }
        }

        if (bestLane == null) return Collections.emptyMap();

        log.info(
                "[AI] Winning pair: {} <-> {}",
                bestLane.getDirectionName(),
                bestLane.getOpposingLane() == null
                        ? "-"
                        : bestLane.getOpposingLane().getDirectionName()
        );

        log.info(
                "[AI] Pair congestion = {}",
                maxPairCongestion
        );

        // 3. Determine signal for each lane
        List<SignalHistory> histories = new ArrayList<>();
        List<SignalMessage> messages = new ArrayList<>();

        // Use the highest congestion level among the winning pair to determine green duration
        double winningCongestion = laneCongestionMap.get(bestLane.getId());
        if (bestLane.getOpposingLane() != null) {
            winningCongestion = Math.max(winningCongestion, laneCongestionMap.get(bestLane.getOpposingLane().getId()));
        }

        int greenDuration = calculateGreenDuration(winningCongestion);
        int redDuration = TOTAL_CYCLE - greenDuration;
        String trafficLevel = getTrafficLevel(winningCongestion);

        log.info(
                "[AI] {} + {} => GREEN={}s RED={}s LEVEL={}",
                bestLane.getDirectionName(),
                bestLane.getOpposingLane().getDirectionName(),
                greenDuration,
                redDuration,
                trafficLevel
        );

        Set<Long> winningPairIds = new HashSet<>();
        winningPairIds.add(bestLane.getId());
        if (bestLane.getOpposingLane() != null) {
            winningPairIds.add(bestLane.getOpposingLane().getId());
        }

        for (Lane lane : lanes) {
            boolean isGreen = winningPairIds.contains(lane.getId());
            
            int currentGreen = isGreen ? greenDuration : redDuration; // Simplification: other lanes are red
            int currentRed = isGreen ? redDuration : greenDuration;
            
            // Note: In a real system, the logic for "the rest must be RED" might be more complex 
            // if there are more than 2 pairs. Here we follow the rule: winning pair GREEN, others RED.
            // If they are RED, their "green duration" for the NEXT phase isn't calculated yet, 
            // so we use the inverse for simplicity in the history record.
            
            String signal = isGreen ? "GREEN" : "RED";

            histories.add(SignalHistory.builder()
                    .intersection(intersection)
                    .lane(lane)
                    .greenDuration(isGreen ? greenDuration : 0) // If RED, green duration is 0 for this snapshot
                    .redDuration(isGreen ? 0 : redDuration)     // Simplified
                    .build());

            messages.add(SignalMessage.builder()
                    .intersectionId(intersection.getId())
                    .laneId(lane.getId())
                    .direction(lane.getDirectionName())
                    .signal(signal)
                    .greenDuration(isGreen ? greenDuration : 0)
                    .redDuration(isGreen ? 0 : redDuration)
                    .remaining(isGreen ? greenDuration : redDuration)
                    .trafficLevel(isGreen ? trafficLevel : getTrafficLevel(laneCongestionMap.get(lane.getId())))
                    .build());

        }

        for (Lane lane : lanes) {
            log.info(
                    "[AI] Lane {} : {}%",
                    lane.getDirectionName(),
                    laneCongestionMap.get(lane.getId())
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("histories", histories);
        result.put("messages", messages);
        return result;
    }

    private int calculateGreenDuration(double congestion) {
        if (congestion <= 30) return 20;
        if (congestion <= 60) return 40;
        return 60;
    }

    private String getTrafficLevel(double congestion) {
        if (congestion <= 30) return "LOW";
        if (congestion <= 60) return "MEDIUM";
        return "HIGH";
    }
}
