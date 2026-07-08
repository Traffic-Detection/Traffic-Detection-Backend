package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.RoadSegmentResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RoadSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Road Segment operations.
 *
 * <p>Provides read-only endpoints for fetching road segment data
 * with current traffic information. Used by the frontend to render
 * traffic-colored polylines on the map.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/roads")
@RequiredArgsConstructor
public class RoadSegmentController {

    private final RoadSegmentService roadSegmentService;

    /**
     * Get all active road segments with their current traffic levels.
     *
     * @return list of road segment responses
     */
    @GetMapping
    public ResponseEntity<List<RoadSegmentResponse>> getAllRoadSegments() {
        log.info("GET /api/roads");
        List<RoadSegmentResponse> segments = roadSegmentService.getAllRoadSegments();
        return ResponseEntity.ok(segments);
    }

    /**
     * Get road segments connected to a specific intersection.
     *
     * @param intersectionId the intersection ID
     * @return list of connected road segment responses
     */
    @GetMapping("/intersection/{intersectionId}")
    public ResponseEntity<List<RoadSegmentResponse>> getRoadSegmentsByIntersection(
            @PathVariable Long intersectionId) {
        log.info("GET /api/roads/intersection/{}", intersectionId);
        List<RoadSegmentResponse> segments = roadSegmentService.getRoadSegmentsByIntersection(intersectionId);
        return ResponseEntity.ok(segments);
    }
}
