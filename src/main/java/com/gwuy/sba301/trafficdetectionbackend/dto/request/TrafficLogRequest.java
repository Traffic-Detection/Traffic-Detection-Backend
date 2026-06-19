package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLogRequest {

    @NotNull(message = "lane_id is required")
    @JsonProperty("lane_id")
    private Long laneId;

    @NotNull(message = "vehicle_count is required")
    @Min(value = 0, message = "vehicle_count must be greater than or equal to 0")
    @JsonProperty("vehicle_count")
    private Integer vehicleCount;

    @NotNull(message = "congestion is required")
    @DecimalMin(value = "0.0", message = "congestion must be at least 0.0")
    @DecimalMax(value = "100.0", message = "congestion must not exceed 100.0")
    @JsonProperty("congestion")
    private Double congestionLevel;
}