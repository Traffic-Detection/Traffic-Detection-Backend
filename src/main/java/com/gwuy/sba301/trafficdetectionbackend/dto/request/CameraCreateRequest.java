package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import com.gwuy.sba301.trafficdetectionbackend.enums.CameraStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraCreateRequest {
    String cameraName;
    String ipAddress;
    String macAddress;
    String serialNumber;
    CameraStatus status;
}
