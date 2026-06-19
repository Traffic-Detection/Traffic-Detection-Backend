package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import com.gwuy.sba301.trafficdetectionbackend.enums.CameraStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CameraResponse {
    private Long id;
    private String ipAddress;
    private CameraStatus status;
    private Long laneId;
}