package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalConfigRequest {

    @NotNull(message = "Intersection ID is required")
    private Long intersectionId;

    @NotNull(message = "Lane ID is required")
    private Long laneId;

    @NotNull(message = "Green duration is required")
    @Min(value = 1, message = "Green duration must be at least 1s")
    private Integer greenDuration;

    @NotNull(message = "Yellow duration is required")
    @Min(value = 1, message = "Yellow duration must be at least 1s")
    private Integer yellowDuration;

    @NotNull(message = "Red duration is required")
    @Min(value = 1, message = "Red duration must be at least 1s")
    private Integer redDuration;
}