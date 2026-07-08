package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.enums.IntersectionStatus;
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
public class IntersectionCreateRequest {
    String name;
    String address;
    Double lat;
    Double lng;
    OperatingMode operatingMode;
    IntersectionStatus status;
}
