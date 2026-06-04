package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLogRequest {
    private Long laneId;
    private Integer vehicleCount;
    private Double congestionLevel;
}
