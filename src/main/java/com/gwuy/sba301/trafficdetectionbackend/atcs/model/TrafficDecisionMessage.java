package com.gwuy.sba301.trafficdetectionbackend.atcs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for real-time dashboard updates.
 * Contains both raw traffic inputs and the resulting AI decisions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficDecisionMessage {
    private TrafficInput northSouth;
    private TrafficInput eastWest;
    private IntersectionDecision decision;
    private LocalDateTime timestamp;
}
