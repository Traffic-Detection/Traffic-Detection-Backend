package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.SignalConfigRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalConfigResponse;

import java.util.List;

public interface SignalConfigService {
    List<SignalConfigResponse> getConfigsByIntersection(Long intersectionId);
    SignalConfigResponse createConfig(SignalConfigRequest request);
    SignalConfigResponse updateConfig(Long id, SignalConfigRequest request);
    void deleteConfig(Long id);
}