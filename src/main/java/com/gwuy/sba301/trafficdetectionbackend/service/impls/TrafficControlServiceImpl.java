package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.UpdateOperatingModeRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.*;
import com.gwuy.sba301.trafficdetectionbackend.entity.*;
import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.enums.CameraStatus;
import com.gwuy.sba301.trafficdetectionbackend.exception.IntersectionNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.exception.LaneNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.*;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ImageStorageService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
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
    private final ModeSwitchManager modeSwitchManager;
    private final Optional<ImageStorageService> imageStorageService;

    private static final int BASE_GREEN_TIME = 30; // Giây
    private static final int MAX_GREEN_TIME = 60; // Giây
    private static final double HIGH_CONGESTION_THRESHOLD = 75.0; // %

    @Override
    @Transactional
    public IntersectionResponse updateOperatingMode(Long intersectionId, UpdateOperatingModeRequest request) {
        OperatingMode newMode = request.getOperatingMode();

        // 1. Lấy thông tin ngã tư ngay từ đầu
        Intersection intersection = intersectionRepository.findById(intersectionId)
                .orElseThrow(() -> new IntersectionNotFoundException(intersectionId));

        List<Lane> lanes = laneRepository.findByIntersectionId(intersectionId);

        // LOGIC 1: Đổi sang MANUAL -> Nếu thiếu Config, TỰ ĐỘNG TẠO MẶC ĐỊNH thay vì báo lỗi
        if (newMode == OperatingMode.MANUAL) {
            List<SignalConfig> configs = signalConfigRepository.findByIntersectionId(intersectionId);

            if (configs.size() != lanes.size()) {
                log.info("Ngã tư {} thiếu cấu hình. Hệ thống tự động sinh cấu hình mặc định cho {} làn", intersectionId, lanes.size());
                if (!configs.isEmpty()) {
                    signalConfigRepository.deleteAll(configs);
                }
                for (Lane lane : lanes) {
                    SignalConfig defaultConfig = SignalConfig.builder()
                            .intersection(intersection)
                            .lane(lane)
                            .greenDuration(40)
                            .yellowDuration(5)
                            .redDuration(40) // Phù hợp tổng chu kỳ mặc định 85s
                            .build();
                    signalConfigRepository.save(defaultConfig);
                }
            }
        }

        // LOGIC 2: Đổi sang AI -> Bắt buộc phải có đủ 4 Camera ACTIVE
        if (newMode == OperatingMode.AI) {
            long activeCameraCount = cameraDeviceRepository.findAll().stream()
                    .filter(c -> c.getLane() != null && c.getLane().getIntersection().getId().equals(intersectionId))
                    .filter(c -> c.getStatus() == CameraStatus.ONLINE)
                    .count();

            if (activeCameraCount < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không đủ 4 camera hoạt động (ACTIVE) tại ngã tư này (Hiện có: " + activeCameraCount + "/4). Không thể kích hoạt AI Adaptive!");
            }
        }

        OperatingMode oldMode = intersection.getOperatingMode();
        intersection.setOperatingMode(newMode);
        intersectionRepository.save(intersection);

        if (oldMode != newMode) {
            log.info("Mode changed from {} to {} for IntersectionId={}", oldMode, newMode, intersectionId);
            manualSignalService.invalidateCache(intersectionId);

            if (oldMode == OperatingMode.MANUAL && newMode == OperatingMode.AI) {
                long cycleDurationMs = calculateManualCycleDurationMs(intersectionId);
                modeSwitchManager.scheduleAiActivation(intersectionId, cycleDurationMs);
            }

            if (oldMode == OperatingMode.AI && newMode == OperatingMode.MANUAL) {
                modeSwitchManager.clearPending(intersectionId);
            }
        }

        return IntersectionResponse.builder()
                .id(intersection.getId())
                .name(intersection.getName())
                .operatingMode(intersection.getOperatingMode())
                .createdAt(intersection.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void recordTrafficLog(TrafficLogRequest request, MultipartFile image) {
        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new LaneNotFoundException(request.getLaneId()));

        ImageStorageService storage = imageStorageService
                .orElseThrow(() -> new RuntimeException("Image storage service is not configured"));
        String frameUrl = storage.upload(image);

        TrafficLog trafficLog = TrafficLog.builder()
                .lane(lane)
                .vehicleCount(request.getVehicleCount())
                .congestionLevel(request.getCongestionLevel())
                .frameUrl(frameUrl)
                .build();

        trafficLogRepository.save(trafficLog);
        log.info("Recorded traffic log for LaneId={}. Congestion: {}%", request.getLaneId(), request.getCongestionLevel());

        TrafficLogResponse response = TrafficLogResponse.builder()
                .id(trafficLog.getId())
                .laneId(trafficLog.getLane().getId())
                .vehicleCount(trafficLog.getVehicleCount())
                .congestionLevel(trafficLog.getCongestionLevel())
                .frameUrl(trafficLog.getFrameUrl())
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
            double currentCongestion = getLatestCongestionLevel(lane.getId());

            int newGreenDuration = BASE_GREEN_TIME;
            int newRedDuration = BASE_GREEN_TIME;

            if (currentCongestion >= HIGH_CONGESTION_THRESHOLD) {
                newGreenDuration = Math.min(BASE_GREEN_TIME + 20, MAX_GREEN_TIME);
                log.info("High congestion detected on LaneId={}. Extending green light to {}s", lane.getId(), newGreenDuration);
            }

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
                        .yellowDuration(history.getYellowDuration())
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
                        .frameUrl(log.getFrameUrl())
                        .recordedAt(log.getRecordedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private long calculateManualCycleDurationMs(Long intersectionId) {
        List<SignalConfig> configs = signalConfigRepository.findByIntersectionId(intersectionId);
        if (configs.isEmpty()) {
            return 0;
        }

        return configs.stream()
                .mapToLong(c -> (long) (c.getGreenDuration() + c.getYellowDuration() + c.getRedDuration()) * 1000L)
                .max()
                .orElse(0);
    }
}