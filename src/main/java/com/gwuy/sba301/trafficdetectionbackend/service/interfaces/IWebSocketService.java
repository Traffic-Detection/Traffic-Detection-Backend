package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficLogResponse;

import java.util.List;

public interface IWebSocketService {
    void sendSignalUpdate(SignalMessage message);
    void sendSignalUpdates(List<SignalMessage> messages);
    void sendTrafficLog(TrafficLogResponse trafficLog);
    void sendTrafficLogs(List<TrafficLogResponse> logs);
}
