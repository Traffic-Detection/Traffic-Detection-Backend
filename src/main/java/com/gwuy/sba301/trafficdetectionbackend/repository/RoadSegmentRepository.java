package com.gwuy.sba301.trafficdetectionbackend.repository;

import com.gwuy.sba301.trafficdetectionbackend.entity.RoadSegment;
import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RoadSegment} entity.
 * Provides queries for route recommendation and traffic simulation.
 */
@Repository
public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {

    /**
     * Find all road segments with a given operational status.
     */
    List<RoadSegment> findByStatus(RoadSegmentStatus status);

    /**
     * Find road segments connected to a specific intersection (either direction).
     */
    List<RoadSegment> findByFromIntersectionIdOrToIntersectionId(Long fromId, Long toId);

    /**
     * Find a road segment by its OpenStreetMap way ID.
     */
    Optional<RoadSegment> findByOsmWayId(Long osmWayId);

    /**
     * Find all road segments with a specific traffic level.
     */
    List<RoadSegment> findByTrafficLevel(TrafficLevel level);
}
