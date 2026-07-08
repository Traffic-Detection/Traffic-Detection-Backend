package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for route recommendation.
 * Contains the GPS coordinates of the start and end locations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendRequest {

    @NotNull(message = "Start latitude is required")
    private Double startLat;

    @NotNull(message = "Start longitude is required")
    private Double startLng;

    @NotNull(message = "End latitude is required")
    private Double endLat;

    @NotNull(message = "End longitude is required")
    private Double endLng;
}
