package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.IOperatingModeGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OperatingModeGuard implements IOperatingModeGuard {

    /**
     * Checks whether AI-based signal processing is allowed for the given intersection.
     *
     * @param intersection the intersection to check
     * @return true if the intersection's operating mode permits AI processing
     */
    @Override
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
