package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;

public interface OperatingModeGuard {
    boolean isAiProcessingAllowed(Intersection intersection);
}
