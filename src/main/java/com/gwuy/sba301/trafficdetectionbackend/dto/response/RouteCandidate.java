package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single route candidate returned by OSRM
 * with traffic scoring details applied by the backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteCandidate {

    /** Route index (0-based) from OSRM alternatives */
    private Integer routeIndex;

    /** Total distance in meters */
    private Double distance;

    /** Total duration in seconds */
    private Double duration;

    /** Combined score: trafficPenalty + distance + duration */
    private Double score;

    /** Sum of all traffic penalties (intersection + road segment) */
    private Double trafficPenalty;

    /** Overall traffic assessment for this route */
    private String trafficLevel;

    /** GeoJSON geometry of the route polyline */
    private Object geometry;

    /** Intersections that this route passes through */
    private List<String> intersections;

    /** Road segments that this route passes through */
    private List<String> roadSegments;
}
