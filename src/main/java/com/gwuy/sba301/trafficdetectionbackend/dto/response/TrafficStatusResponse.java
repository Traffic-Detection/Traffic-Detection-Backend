package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficStatusResponse {

    private Long laneId;
    private String directionName;
    private Integer vehicleCount;
    private Double congestionLevel;
    private Long opposingLaneId;
}