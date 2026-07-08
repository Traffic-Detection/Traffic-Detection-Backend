package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.RoadSegmentResponse;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;

import java.util.List;

/**
 * Service interface for road segment operations.
 * Provides CRUD and traffic level management for road segments.
 */
public interface RoadSegmentService {

    /**
     * Get all active road segments.
     *
     * @return list of road segment responses
     */
    List<RoadSegmentResponse> getAllRoadSegments();

    /**
     * Get road segments connected to a specific intersection.
     *
     * @param intersectionId the intersection ID
     * @return list of connected road segments
     */
    List<RoadSegmentResponse> getRoadSegmentsByIntersection(Long intersectionId);

    /**
     * Update the traffic level of a specific road segment.
     *
     * @param roadSegmentId the road segment ID
     * @param trafficLevel  the new traffic level
     */
    void updateTrafficLevel(Long roadSegmentId, TrafficLevel trafficLevel);
}
