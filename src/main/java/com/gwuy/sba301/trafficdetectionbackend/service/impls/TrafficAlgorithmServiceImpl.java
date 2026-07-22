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
        log.info("[AI] Bắt đầu tính toán chu kỳ 80s cho ngã tư: {}", intersection.getName());

        List<Lane> lanes = laneRepository.findByIntersectionId(intersection.getId());
        if (lanes.isEmpty()) return Collections.emptyMap();

        // 1. Quét Data kẹt xe mới nhất
        Map<Long, Double> laneCongestionMap = new HashMap<>();
        for (Lane lane : lanes) {
            double congestion = trafficLogRepository.findFirstByLaneIdOrderByRecordedAtDesc(lane.getId())
                    .map(TrafficLog::getCongestionLevel).orElse(0.0);
            laneCongestionMap.put(lane.getId(), congestion);
        }

        // 2. Tìm trục đường kẹt nhất
        Lane bestLane = null;
        double maxPairCongestion = -1;
        Set<Long> visited = new HashSet<>();

        for (Lane lane : lanes) {
            if (visited.contains(lane.getId())) continue;
            double laneCongestion = laneCongestionMap.getOrDefault(lane.getId(), 0.0);
            double opposingCongestion = lane.getOpposingLane() != null ?
                    laneCongestionMap.getOrDefault(lane.getOpposingLane().getId(), 0.0) : 0.0;

            visited.add(lane.getId());
            if (lane.getOpposingLane() != null) visited.add(lane.getOpposingLane().getId());

            double pairCongestion = laneCongestion + opposingCongestion;
            if (pairCongestion > maxPairCongestion) {
                maxPairCongestion = pairCongestion;
                bestLane = lane;
            }
        }

        if (bestLane == null) return Collections.emptyMap();

        // 3. Tính thời gian cho phe Thắng (Pha 1) và phe Thua (Pha 2)
        double winningCongestion = laneCongestionMap.get(bestLane.getId());
        if (bestLane.getOpposingLane() != null) {
            winningCongestion = Math.max(winningCongestion, laneCongestionMap.get(bestLane.getOpposingLane().getId()));
        }

        // Phe đông xe được đi Xanh nhiều (VD: 60s), phe ít xe đi Xanh ít (VD: 20s)
        int winnerGreen = calculateGreenDuration(winningCongestion);
        int loserGreen = TOTAL_CYCLE - winnerGreen;

        Set<Long> winningPairIds = new HashSet<>();
        winningPairIds.add(bestLane.getId());
        if (bestLane.getOpposingLane() != null) winningPairIds.add(bestLane.getOpposingLane().getId());

        List<SignalHistory> histories = new ArrayList<>();
        List<SignalMessage> messages = new ArrayList<>();

        for (Lane lane : lanes) {
            boolean isWinner = winningPairIds.contains(lane.getId());

            // Logic Lật Pha:
            // - Phe thắng: Xanh trước (Pha 1), Đỏ chờ phe thua đi (Pha 2)
            // - Phe thua: Đỏ chờ phe thắng đi (Pha 1), Xanh sau (Pha 2)
            String initialSignal = isWinner ? "GREEN" : "RED";
            int laneGreenDuration = isWinner ? winnerGreen : loserGreen;
            int laneRedDuration = isWinner ? loserGreen : winnerGreen;

            // Để frontend của cả ngã tư đếm lùi CÙNG NHAU ở Pha 1, remaining = thời gian Pha 1
            int phase1Duration = winnerGreen;

            histories.add(SignalHistory.builder()
                    .intersection(intersection)
                    .lane(lane)
                    .greenDuration(laneGreenDuration)
                    .yellowDuration(DEFAULT_YELLOW_DURATION)
                    .redDuration(laneRedDuration)
                    .build());

            messages.add(SignalMessage.builder()
                    .intersectionId(intersection.getId())
                    .laneId(lane.getId())
                    .direction(lane.getDirectionName())
                    .signal(initialSignal)
                    .greenDuration(laneGreenDuration)
                    .yellowDuration(DEFAULT_YELLOW_DURATION)
                    .redDuration(laneRedDuration)
                    .remaining(phase1Duration) // Cực kỳ quan trọng
                    .trafficLevel(getTrafficLevel(laneCongestionMap.get(lane.getId())))
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