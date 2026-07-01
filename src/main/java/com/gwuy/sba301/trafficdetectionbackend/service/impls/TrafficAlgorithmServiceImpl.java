package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.Lane;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalHistory;
import com.gwuy.sba301.trafficdetectionbackend.entity.TrafficLog;
import com.gwuy.sba301.trafficdetectionbackend.repository.LaneRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.TrafficLogRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficAlgorithmServiceImpl implements TrafficAlgorithmService {

    private final LaneRepository laneRepository;
    private final TrafficLogRepository trafficLogRepository;

    private static final int TOTAL_CYCLE = 80;
    private static final int DEFAULT_YELLOW_DURATION = 5;

    @Override
    public Map<String, Object> calculateAdaptiveSignal(Intersection intersection) {
        log.info("[AI] Processing intersection: {}", intersection.getName());

        List<Lane> lanes = laneRepository.findByIntersectionId(intersection.getId());
        if (lanes.isEmpty()) {
            log.warn("[AI] No lanes found for intersection: {}", intersection.getName());
            return Collections.emptyMap();
        }

        Map<Long, Double> laneCongestionMap = new HashMap<>();
        for (Lane lane : lanes) {
            double congestion = trafficLogRepository.findFirstByLaneIdOrderByRecordedAtDesc(lane.getId())
                    .map(TrafficLog::getCongestionLevel)
                    .orElse(0.0);
            laneCongestionMap.put(lane.getId(), congestion);
        }

        Lane bestLane = null;
        double maxPairCongestion = -1;
        Set<Long> visited = new HashSet<>();

        for (Lane lane : lanes) {
            if (visited.contains(lane.getId())) continue;

            double laneCongestion = laneCongestionMap.getOrDefault(lane.getId(), 0.0);
            double opposingCongestion = 0.0;

            if (lane.getOpposingLane() != null) {
                opposingCongestion = laneCongestionMap.getOrDefault(lane.getOpposingLane().getId(), 0.0);
                visited.add(lane.getOpposingLane().getId());
            }

            visited.add(lane.getId());
            double pairCongestion = laneCongestion + opposingCongestion;

            if (pairCongestion > maxPairCongestion) {
                maxPairCongestion = pairCongestion;
                bestLane = lane;
            }
        }

        if (bestLane == null) return Collections.emptyMap();

        List<SignalHistory> histories = new ArrayList<>();
        List<SignalMessage> messages = new ArrayList<>();

        double winningCongestion = laneCongestionMap.get(bestLane.getId());
        if (bestLane.getOpposingLane() != null) {
            winningCongestion = Math.max(winningCongestion, laneCongestionMap.get(bestLane.getOpposingLane().getId()));
        }

        int greenDuration = calculateGreenDuration(winningCongestion);
        int redDuration = TOTAL_CYCLE - greenDuration;
        String trafficLevel = getTrafficLevel(winningCongestion);

        Set<Long> winningPairIds = new HashSet<>();
        winningPairIds.add(bestLane.getId());
        if (bestLane.getOpposingLane() != null) {
            winningPairIds.add(bestLane.getOpposingLane().getId());
        }

        for (Lane lane : lanes) {
            boolean isGreen = winningPairIds.contains(lane.getId());
            String signal = isGreen ? "GREEN" : "RED";

            histories.add(SignalHistory.builder()
                    .intersection(intersection)
                    .lane(lane)
                    .greenDuration(greenDuration)
                    .yellowDuration(DEFAULT_YELLOW_DURATION)
                    .redDuration(redDuration)
                    .build());

            messages.add(SignalMessage.builder()
                    .intersectionId(intersection.getId())
                    .laneId(lane.getId())
                    .direction(lane.getDirectionName())
                    .signal(signal)
                    .greenDuration(greenDuration)
                    .yellowDuration(DEFAULT_YELLOW_DURATION)
                    .redDuration(redDuration)
                    // Tất cả các Làn phải lấy greenDuration làm mốc đếm ngược Pha 1
                    .remaining(greenDuration)
                    .trafficLevel(isGreen ? trafficLevel : getTrafficLevel(laneCongestionMap.get(lane.getId())))
                    .build());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("histories", histories);
        result.put("messages", messages);
        return result;
    }

    @Override
    public int calculateGreenDuration(double congestion) {
        if (congestion <= 30) return 20;
        if (congestion <= 60) return 40;
        return 60;
    }

    @Override
    public String getTrafficLevel(double congestion) {
        if (congestion <= 30) return "LOW";
        if (congestion <= 60) return "MEDIUM";
        return "HIGH";
    }
}