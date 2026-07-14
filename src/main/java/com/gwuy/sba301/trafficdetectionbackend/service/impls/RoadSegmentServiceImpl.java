package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.RoadSegmentResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.RoadSegment;
import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
import com.gwuy.sba301.trafficdetectionbackend.exception.RoadSegmentNotFoundException;
import com.gwuy.sba301.trafficdetectionbackend.repository.RoadSegmentRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RoadSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RoadSegmentService}.
 * Provides read operations and traffic level updates for road segments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadSegmentServiceImpl implements RoadSegmentService {

    private final RoadSegmentRepository roadSegmentRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoadSegmentResponse> getAllRoadSegments() {
        log.info("Fetching all active road segments");
        return roadSegmentRepository.findByStatus(RoadSegmentStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoadSegmentResponse> getRoadSegmentsByIntersection(Long intersectionId) {
        log.info("Fetching road segments for intersection {}", intersectionId);
        return roadSegmentRepository.findByFromIntersectionIdOrToIntersectionId(intersectionId, intersectionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateTrafficLevel(Long roadSegmentId, TrafficLevel trafficLevel) {
        RoadSegment segment = roadSegmentRepository.findById(roadSegmentId)
                .orElseThrow(() -> new RoadSegmentNotFoundException(
                        "Road segment not found with id: " + roadSegmentId));

        // Calculate traffic cost based on level
        double trafficCost = calculateTrafficCost(trafficLevel);

        segment.setTrafficLevel(trafficLevel);
        segment.setTrafficCost(trafficCost);
        roadSegmentRepository.save(segment);

        log.debug("Updated road segment {} traffic to {} (cost={})",
                roadSegmentId, trafficLevel, trafficCost);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Calculates the traffic cost multiplier based on the traffic level.
     */
    private double calculateTrafficCost(TrafficLevel level) {
        return switch (level) {
            case HIGH -> 500.0;
            case MEDIUM -> 50.0;
            case LOW -> 5.0;
        };
    }

    /**
     * Maps a RoadSegment entity to its response DTO.
     */
    private RoadSegmentResponse mapToResponse(RoadSegment entity) {
        return RoadSegmentResponse.builder()
                .id(entity.getId())
                .roadName(entity.getRoadName())
                .fromIntersectionId(entity.getFromIntersection().getId())
                .fromIntersectionName(entity.getFromIntersection().getName())
                .toIntersectionId(entity.getToIntersection().getId())
                .toIntersectionName(entity.getToIntersection().getName())
                .distance(entity.getDistance())
                .speedLimit(entity.getSpeedLimit())
                .osmWayId(entity.getOsmWayId())
                .trafficLevel(entity.getTrafficLevel().name())
                .trafficCost(entity.getTrafficCost())
                .status(entity.getStatus().name())
                .build();
    }
}
