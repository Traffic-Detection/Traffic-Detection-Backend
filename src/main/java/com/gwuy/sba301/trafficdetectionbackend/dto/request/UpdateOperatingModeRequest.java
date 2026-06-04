package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOperatingModeRequest {

    private OperatingMode operatingMode;
}