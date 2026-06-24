package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficInput {
    private String laneName;
    private int vehicleCount;
    private double congestionLevel;
}
