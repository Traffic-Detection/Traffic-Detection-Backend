package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;

import java.util.Map;

public interface TrafficAlgorithmService {
    Map<String, Object> calculateAdaptiveSignal(Intersection intersection);
    int calculateGreenDuration(double congestion);
    String getTrafficLevel(double congestion);
}
