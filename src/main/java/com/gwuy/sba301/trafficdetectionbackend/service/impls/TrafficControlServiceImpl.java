package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.UpdateOperatingModeRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.*;
import com.gwuy.sba301.trafficdetectionbackend.entity.*;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.exception.IntersectionNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.exception.LaneNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.*;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficControlServiceImpl implements TrafficControlService {

    private final IntersectionRepository intersectionRepository;
    private final LaneRepository laneRepository;
    private final TrafficLogRepository trafficLogRepository;
    private final SignalHistoryRepository signalHistoryRepository;
    private final CameraDeviceRepository cameraDeviceRepository;
    private final OperatingModeGuard operatingModeGuard;
    private final WebSocketServiceImpl webSocketServiceImpl;
    private final ManualSignalService manualSignalService;
    private final SignalConfigRepository signalConfigRepository;

    private static final int BASE_GREEN_TIME = 30; // Giây
    private static final int MAX_GREEN_TIME = 60; // Giây
    private static final double HIGH_CONGESTION_THRESHOLD = 75.0; // %

    @Override
    @Transactional
    public IntersectionResponse updateOperatingMode(Long intersectionId, UpdateOperatingModeRequest request) {
        OperatingMode newMode = request.getOperatingMode();

        if (newMode == OperatingMode.MANUAL) {
            List<Lane> lanes = laneRepository.findByIntersectionId(intersectionId);
            List<SignalConfig> configs = signalConfigRepository.findByIntersectionId(intersectionId);

            if (configs.isEmpty() || configs.size() != lanes.size()) {
                throw new RuntimeException("Lỗi: Chưa thiết lập đủ cấu hình thời lượng đèn (signal_configs) cho tất cả các làn. Không thể chuyển sang chế độ MANUAL!");
            }
        }

        Intersection intersection = intersectionRepository.findById(intersectionId)
                .orElseThrow(() -> {
                    log.error("Failed to update mode. IntersectionId={} not found", intersectionId);
                    return new IntersectionNotFoundException(intersectionId);
                });

        OperatingMode oldMode = intersection.getOperatingMode();

        intersection.setOperatingMode(newMode);
        intersectionRepository.save(intersection);

        if (oldMode != newMode) {
            log.info("Mode changed from {} to {} for IntersectionId={}. Invalidating manual cache.",
                    oldMode, newMode, intersectionId);
            manualSignalService.invalidateCache(intersectionId);
        }

        log.info("Successfully updated operating mode for IntersectionId={} to {}", intersectionId, newMode);

        return IntersectionResponse.builder()
                .id(intersection.getId())
                .name(intersection.getName())
                .operatingMode(intersection.getOperatingMode())
                .createdAt(intersection.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void recordTrafficLog(TrafficLogRequest request) {
        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new LaneNotFoundException(request.getLaneId()));

        TrafficLog trafficLog = TrafficLog.builder()
                .lane(lane)
                .vehicleCount(request.getVehicleCount())
                .congestionLevel(request.getCongestionLevel())
                .build();

        trafficLogRepository.save(trafficLog);
        log.info("Recorded traffic log for LaneId={}. Congestion: {}%", request.getLaneId(), request.getCongestionLevel());

        // Broadcast new log to WebSocket subscribers via /topic/traffic-logs
        TrafficLogResponse response = TrafficLogResponse.builder()
                .id(trafficLog.getId())
                .laneId(trafficLog.getLane().getId())
                .vehicleCount(trafficLog.getVehicleCount())
                .congestionLevel(trafficLog.getCongestionLevel())
                .recordedAt(trafficLog.getRecordedAt())
                .build();
        webSocketServiceImpl.sendTrafficLog(response);
    }

    @Override
    @Transactional
    public void processAdaptiveSignals(Long intersectionId) {
        Intersection intersection = intersectionRepository.findById(intersectionId)
                .orElseThrow(() -> new IntersectionNotFoundException(intersectionId));

        if (!operatingModeGuard.isAiProcessingAllowed(intersection)) {
            log.info("Skipping AI signal processing. IntersectionId={} is in {} mode.",
                    intersectionId, intersection.getOperatingMode());
            return;
        }

        List<Lane> lanes = laneRepository.findByIntersectionId(intersectionId);

        for (Lane lane : lanes) {
            // Lấy độ kẹt xe mới nhất của làn hiện tại
            double currentCongestion = getLatestCongestionLevel(lane.getId());

            int newGreenDuration = BASE_GREEN_TIME;
            int newRedDuration = BASE_GREEN_TIME;

            // Logic AI cơ bản: Tự động kéo dài đèn xanh nếu kẹt cứng
            if (currentCongestion >= HIGH_CONGESTION_THRESHOLD) {
                newGreenDuration = Math.min(BASE_GREEN_TIME + 20, MAX_GREEN_TIME);
                log.info("High congestion detected on LaneId={}. Extending green light to {}s", lane.getId(), newGreenDuration);
            }

            // Lưu lịch sử thay đổi đèn
            SignalHistory signalHistory = SignalHistory.builder()
                    .intersection(intersection)
                    .lane(lane)
                    .greenDuration(newGreenDuration)
                    .yellowDuration(3)
                    .redDuration(newRedDuration)
                    .build();

            signalHistoryRepository.save(signalHistory);
        }

        log.info("Completed adaptive signal processing for IntersectionId={}", intersectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntersectionResponse> getAllIntersections() {
        return intersectionRepository.findAll().stream()
                .map(intersection -> IntersectionResponse.builder()
                        .id(intersection.getId())
                        .name(intersection.getName())
                        .operatingMode(intersection.getOperatingMode())
                        .createdAt(intersection.getCreatedAt())
                        .build())
                .toList();
    }

    private double getLatestCongestionLevel(Long laneId) {
        return trafficLogRepository.findFirstByLaneIdOrderByRecordedAtDesc(laneId)
                .map(TrafficLog::getCongestionLevel)
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaneResponse> getLanesByIntersection(Long intersectionId) {
        return laneRepository.findByIntersectionId(intersectionId).stream()
                .map(lane -> LaneResponse.builder()
                        .id(lane.getId())
                        .directionName(lane.getDirectionName())
                        .opposingLaneId(lane.getOpposingLane() != null ? lane.getOpposingLane().getId() : null)
                        .intersectionId(lane.getIntersection().getId())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalHistoryResponse> getSignalHistoryByIntersection(Long intersectionId) {
        return signalHistoryRepository.findByIntersectionIdOrderByAppliedAtDesc(intersectionId).stream()
                .map(history -> SignalHistoryResponse.builder()
                        .id(history.getId())
                        .intersectionId(history.getIntersection().getId())
                        .laneId(history.getLane().getId())
                        .greenDuration(history.getGreenDuration())
                        .redDuration(history.getRedDuration())
                        .appliedAt(history.getAppliedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CameraResponse> getAllCameras() {
        return cameraDeviceRepository.findAll().stream()
                .map(camera -> CameraResponse.builder()
                        .id(camera.getId())
                        .ipAddress(camera.getIpAddress())
                        .status(camera.getStatus())
                        .laneId(camera.getLane().getId())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrafficLogResponse> getAllTrafficLogs() {
        return trafficLogRepository.findAll().stream()
                .map(log -> TrafficLogResponse.builder()
                        .id(log.getId())
                        .laneId(log.getLane().getId())
                        .vehicleCount(log.getVehicleCount())
                        .congestionLevel(log.getCongestionLevel())
                        .recordedAt(log.getRecordedAt())
                        .build())
                .collect(Collectors.toList());
    }
}