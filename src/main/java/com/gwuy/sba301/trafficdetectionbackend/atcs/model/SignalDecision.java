package com.gwuy.sba301.trafficdetectionbackend.atcs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalDecision {
    private String laneName;
    private int greenDuration;
    private int redDuration;
    private String reason;
}
