package com.gwuy.sba301.trafficdetectionbackend.atcs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a synchronized decision for an intersection, 
 * covering both North-South and East-West directions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionDecision {
    private SignalDecision northSouthDecision;
    private SignalDecision eastWestDecision;
}
