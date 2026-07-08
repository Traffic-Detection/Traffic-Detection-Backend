package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.RouteRecommendRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.RouteHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.RouteRecommendResponse;

import java.util.List;

/**
 * Service interface for route recommendation operations.
 * Integrates with OSRM for routing and applies traffic-aware scoring.
 */
public interface RouteService {

    /**
     * Recommend the best route based on traffic conditions.
     * Calls OSRM with alternatives=3, scores each route using
     * intersection and road segment traffic levels, and returns
     * the route with the lowest combined score.
     *
     * @param request the start and end GPS coordinates
     * @return the recommendation response with all candidates and selected route
     */
    RouteRecommendResponse recommendRoute(RouteRecommendRequest request);

    /**
     * Retrieve the route recommendation history (most recent 20 records).
     *
     * @return list of historical route recommendations
     */
    List<RouteHistoryResponse> getRouteHistory();
}
