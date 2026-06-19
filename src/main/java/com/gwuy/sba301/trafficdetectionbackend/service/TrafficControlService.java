package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.UpdateOperatingModeRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.CameraResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.IntersectionResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.LaneResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;

import java.util.List;

public interface TrafficControlService {
    IntersectionResponse updateOperatingMode(Long intersectionId, UpdateOperatingModeRequest request);
    void recordTrafficLog(TrafficLogRequest request);
    void processAdaptiveSignals(Long intersectionId);
    List<IntersectionResponse> getAllIntersections();
    List<LaneResponse> getLanesByIntersection(Long intersectionId);
    List<SignalHistoryResponse> getSignalHistoryByIntersection(Long intersectionId);
    List<CameraResponse> getAllCameras();
}