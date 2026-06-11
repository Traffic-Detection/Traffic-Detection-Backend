package com.gwuy.sba301.trafficdetectionbackend.atcs.algorithm;

import com.gwuy.sba301.trafficdetectionbackend.atcs.model.IntersectionDecision;
import com.gwuy.sba301.trafficdetectionbackend.atcs.model.SignalDecision;
import com.gwuy.sba301.trafficdetectionbackend.atcs.model.TrafficInput;
import org.springframework.stereotype.Service;

/**
 * Service responsible for calculating adaptive traffic signal timings.
 */
@Service
public class AdaptiveSignalService {

    private static final int BASE_GREEN = 60;
    private static final int BASE_RED = 60;
    private static final int MIN_RED = 15;
    private static final int MAX_GREEN = 120;

    /**
     * Calculates synchronized signal timings for an intersection based on congestion levels.
     *
     * @param northSouth Traffic input for the North-South lane
     * @param eastWest   Traffic input for the East-West lane
     * @return IntersectionDecision containing synchronized timings for both directions
     */
    public IntersectionDecision calculateIntersectionDecision(TrafficInput northSouth, TrafficInput eastWest) {
        SignalDecision nsDecision;
        SignalDecision ewDecision;

        if (northSouth.getCongestionLevel() > eastWest.getCongestionLevel()) {
            // Apply adaptive timing to North-South
            nsDecision = calculateSignalTiming(northSouth, eastWest);
            // East-West timing is the inverse of North-South to maintain cycle synchronization
            ewDecision = new SignalDecision(
                    eastWest.getLaneName(),
                    nsDecision.getRedDuration(),
                    nsDecision.getGreenDuration(),
                    "Derived from North-South synchronization."
            );
        } else {
            // Apply adaptive timing to East-West
            ewDecision = calculateSignalTiming(eastWest, northSouth);
            // North-South timing is the inverse of East-West
            nsDecision = new SignalDecision(
                    northSouth.getLaneName(),
                    ewDecision.getRedDuration(),
                    ewDecision.getGreenDuration(),
                    "Derived from East-West synchronization."
            );
        }

        return new IntersectionDecision(nsDecision, ewDecision);
    }

    /**
     * Calculates the signal decision based on the congestion level of the current lane and the opposing lane.
     *
     * @param currentLane  Traffic input of the current lane
     * @param opposingLane Traffic input of the opposing lane
     * @return SignalDecision containing the adjusted green and red durations
     */
    public SignalDecision calculateSignalTiming(TrafficInput currentLane, TrafficInput opposingLane) {
        double difference = currentLane.getCongestionLevel() - opposingLane.getCongestionLevel();

        // Safety Requirement: If opposing lane has equal or higher congestion, return base timing immediately.
        if (difference <= 0) {
            return new SignalDecision(
                    currentLane.getLaneName(),
                    BASE_GREEN,
                    BASE_RED,
                    "Opposing lane has equal or higher congestion."
            );
        }

        int extraTime = 0;
        String reason = "Normal traffic, applying base timing.";

        // Adjustment Rules
        if (difference >= 50) {
            extraTime = 30;
            reason = "High congestion difference (>= 50), adding +30 seconds green.";
        } else if (difference >= 30) {
            extraTime = 20;
            reason = "Moderate congestion difference (>= 30), adding +20 seconds green.";
        } else if (difference >= 10) {
            extraTime = 10;
            reason = "Slight congestion difference (>= 10), adding +10 seconds green.";
        }

        // Apply extra time
        int greenDuration = BASE_GREEN + extraTime;
        int redDuration = BASE_RED - extraTime;

        // Apply safety constraints
        if (greenDuration > MAX_GREEN) {
            greenDuration = MAX_GREEN;
        }
        if (redDuration < MIN_RED) {
            redDuration = MIN_RED;
        }

        return new SignalDecision(
                currentLane.getLaneName(),
                greenDuration,
                redDuration,
                reason
        );
    }
}
