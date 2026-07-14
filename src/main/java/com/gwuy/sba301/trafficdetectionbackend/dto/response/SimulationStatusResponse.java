package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for simulation start/stop API endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationStatusResponse {

    /** Whether the simulation is currently running */
    private Boolean running;

    /** Human-readable status message */
    private String message;
}
