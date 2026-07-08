package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for route recommendation.
 * Contains the list of candidate routes evaluated by the scoring algorithm,
 * the index of the selected (best) route, and a human-readable message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendResponse {

    /** Index of the recommended route (0-based) in the candidates list */
    private Integer selectedRouteIndex;

    /** Total number of routes returned by OSRM */
    private Integer totalRoutes;

    /** Human-readable recommendation message */
    private String message;

    /** All candidate routes with their scoring details */
    private List<RouteCandidate> candidates;
}
