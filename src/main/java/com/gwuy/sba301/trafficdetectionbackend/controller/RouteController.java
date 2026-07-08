package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.RouteRecommendRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.RouteHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.RouteRecommendResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Route Recommendation operations.
 *
 * <p>Exposes endpoints to request route recommendations based on
 * start/end GPS coordinates and to retrieve recommendation history.</p>
 *
 * <p>All routing logic (OSRM integration, traffic scoring) is delegated
 * to {@link RouteService}. No business logic is placed in this controller.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * Recommend the best route based on current traffic conditions.
     *
     * <p>Calls OSRM with alternatives=3, scores each route using
     * intersection and road segment traffic levels, and returns
     * the route with the lowest combined score.</p>
     *
     * @param request the start and end GPS coordinates
     * @return the recommendation response with all candidates and selected route
     */
    @PostMapping("/recommend")
    public ResponseEntity<RouteRecommendResponse> recommendRoute(
            @Valid @RequestBody RouteRecommendRequest request) {
        log.info("POST /api/routes/recommend — start=({},{}) end=({},{})",
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng());

        RouteRecommendResponse response = routeService.recommendRoute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the route recommendation history (most recent 20 records).
     *
     * @return list of historical route recommendations
     */
    @GetMapping("/history")
    public ResponseEntity<List<RouteHistoryResponse>> getRouteHistory() {
        log.info("GET /api/routes/history");
        List<RouteHistoryResponse> history = routeService.getRouteHistory();
        return ResponseEntity.ok(history);
    }
}
