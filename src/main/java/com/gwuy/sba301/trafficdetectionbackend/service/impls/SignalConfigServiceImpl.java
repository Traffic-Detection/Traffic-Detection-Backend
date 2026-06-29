package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.SignalConfigRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalConfigResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.entity.Lane;
import com.gwuy.sba301.trafficdetectionbackend.entity.SignalConfig;
import com.gwuy.sba301.trafficdetectionbackend.exception.IntersectionNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.exception.LaneNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.IntersectionRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.LaneRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.SignalConfigRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.ManualSignalService;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalConfigServiceImpl implements SignalConfigService {

    private final SignalConfigRepository signalConfigRepository;
    private final IntersectionRepository intersectionRepository;
    private final LaneRepository laneRepository;
    private final ManualSignalService manualSignalService;

    @Override
    @Transactional(readOnly = true)
    public List<SignalConfigResponse> getConfigsByIntersection(Long intersectionId) {
        return signalConfigRepository.findByIntersectionId(intersectionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SignalConfigResponse createConfig(SignalConfigRequest request) {
        Intersection intersection = intersectionRepository.findById(request.getIntersectionId())
                .orElseThrow(() -> new IntersectionNotFoundException(request.getIntersectionId()));

        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new LaneNotFoundException(request.getLaneId()));

        if (signalConfigRepository.findByIntersectionIdAndLaneId(intersection.getId(), lane.getId()).isPresent()) {
            throw new RuntimeException("Cấu hình đèn cho làn đường này đã tồn tại!");
        }

        SignalConfig config = SignalConfig.builder()
                .intersection(intersection)
                .lane(lane)
                .greenDuration(request.getGreenDuration())
                .yellowDuration(request.getYellowDuration())
                .redDuration(request.getRedDuration())
                .build();

        config = signalConfigRepository.save(config);
        manualSignalService.invalidateCache(intersection.getId());

        log.info("Created new SignalConfig for IntersectionId={} LaneId={}", intersection.getId(), lane.getId());
        return mapToResponse(config);
    }

    @Override
    @Transactional
    public SignalConfigResponse updateConfig(Long id, SignalConfigRequest request) {
        SignalConfig config = signalConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình tín hiệu đèn!"));

        config.setGreenDuration(request.getGreenDuration());
        config.setYellowDuration(request.getYellowDuration());
        config.setRedDuration(request.getRedDuration());

        config = signalConfigRepository.save(config);
        manualSignalService.invalidateCache(config.getIntersection().getId());

        log.info("Updated SignalConfig Id={}", id);
        return mapToResponse(config);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        SignalConfig config = signalConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình tín hiệu đèn!"));

        Long intersectionId = config.getIntersection().getId();
        signalConfigRepository.delete(config);
        manualSignalService.invalidateCache(intersectionId);

        log.info("Deleted SignalConfig Id={}", id);
    }

    private SignalConfigResponse mapToResponse(SignalConfig config) {
        return SignalConfigResponse.builder()
                .id(config.getId())
                .intersectionId(config.getIntersection().getId())
                .laneId(config.getLane().getId())
                .greenDuration(config.getGreenDuration())
                .yellowDuration(config.getYellowDuration())
                .redDuration(config.getRedDuration())
                .build();
    }
}