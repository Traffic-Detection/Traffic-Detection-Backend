package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.UpdateOperatingModeRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.CameraResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.IntersectionResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.LaneResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficLogResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TrafficControlService {
    IntersectionResponse updateOperatingMode(Long intersectionId, UpdateOperatingModeRequest request);
    void recordTrafficLog(TrafficLogRequest request, MultipartFile image);
    void processAdaptiveSignals(Long intersectionId);
    List<IntersectionResponse> getAllIntersections();
    List<LaneResponse> getLanesByIntersection(Long intersectionId);
    List<SignalHistoryResponse> getSignalHistoryByIntersection(Long intersectionId);
    List<CameraResponse> getAllCameras();
    List<TrafficLogResponse> getAllTrafficLogs();

    IntersectionResponse createIntersection(IntersectionCreateRequest request);
    LaneResponse createLane(Long intersectionId, LaneCreateRequest request);
    CameraResponse createCamera(Long laneId, CameraCreateRequest request);

    CurrentTrafficResponse getCurrentTraffic();
}
