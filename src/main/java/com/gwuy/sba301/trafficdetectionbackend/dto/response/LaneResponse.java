package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LaneResponse {
    private Long id;
    private String directionName;
    private Long opposingLaneId;
}