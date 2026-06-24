package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.SignalDecision;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficInput;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.IntersectionDecision;

public interface AdaptiveSignalService {
    IntersectionDecision calculateIntersectionDecision(TrafficInput northSouth, TrafficInput eastWest);
    SignalDecision calculateSignalTiming(TrafficInput currentLane, TrafficInput opposingLane);
}
