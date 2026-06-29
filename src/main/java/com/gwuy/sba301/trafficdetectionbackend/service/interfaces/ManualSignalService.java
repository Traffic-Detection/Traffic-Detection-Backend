package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.entity.Intersection;

import java.util.List;

public interface ManualSignalService {
    List<SignalMessage> getFixedSignals(Intersection intersection);
    void invalidateCache(Long intersectionId);
    void invalidateAllCache();
}