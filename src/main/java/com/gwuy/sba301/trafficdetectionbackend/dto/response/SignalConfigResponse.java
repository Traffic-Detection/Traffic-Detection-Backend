package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignalConfigResponse {
    private Long id;
    private Long intersectionId;
    private Long laneId;
    private Integer greenDuration;
    private Integer yellowDuration;
    private Integer redDuration;
}