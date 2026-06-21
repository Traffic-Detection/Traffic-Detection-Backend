package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrafficLogResponse {
    private Long id;
    private Long laneId;
    private Integer vehicleCount;
    private Double congestionLevel;
    private LocalDateTime recordedAt;
}
