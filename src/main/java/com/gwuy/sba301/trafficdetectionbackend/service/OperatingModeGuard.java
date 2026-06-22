package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized guard service for operating mode checks.
 * <p>
 * Encapsulates the business rule (BR-020): AI processing is only permitted
 * when an intersection's operating mode allows it. All components that need
 * to verify mode eligibility should delegate to this service instead of
 * performing inline checks.
 * </p>
 */
@Slf4j
@Service
public class OperatingModeGuard {

    /**
     * Checks whether AI-based signal processing is allowed for the given intersection.
     *
     * @param intersection the intersection to check
     * @return true if the intersection's operating mode permits AI processing
     */
    public boolean isAiProcessingAllowed(Intersection intersection) {
        boolean allowed = intersection.getOperatingMode().isAiAllowed();

        if (!allowed) {
            log.info("[ModeGuard] Intersection {} ({}) blocked - mode: {}",
                    intersection.getId(),
                    intersection.getName(),
                    intersection.getOperatingMode());
        }

        return allowed;
    }
}
