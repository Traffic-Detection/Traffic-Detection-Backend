package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for route recommendation history records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteHistoryResponse {

    private Long id;
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
    private Integer selectedRouteIndex;
    private Integer totalRoutes;
    private Double totalScore;
    private Double totalDistance;
    private Double totalDuration;
    private String routeGeometry;
    private LocalDateTime createdAt;
}
