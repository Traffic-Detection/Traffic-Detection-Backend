package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for an intersection's current traffic level.
 * The traffic level is computed from the in-memory simulation data
 * or from the latest TrafficLog records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionTrafficResponse {

    private Long id;
    private String name;
    private String address;
    private String coordinates;
    private String trafficLevel;
    private Integer vehicleCount;
}
