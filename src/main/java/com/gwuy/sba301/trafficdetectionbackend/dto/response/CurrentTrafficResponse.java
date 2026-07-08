package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregate response for the current traffic status across
 * all intersections and road segments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTrafficResponse {

    /** Current traffic status of all intersections */
    private List<IntersectionTrafficResponse> intersections;

    /** Current traffic status of all active road segments */
    private List<RoadSegmentResponse> roadSegments;

    /** Whether the traffic simulation is currently running */
    private Boolean simulationRunning;
}
