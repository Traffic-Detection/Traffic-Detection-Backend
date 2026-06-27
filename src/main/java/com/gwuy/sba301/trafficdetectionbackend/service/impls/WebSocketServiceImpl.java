package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficLogResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendSignalUpdate(SignalMessage message) {
        messagingTemplate.convertAndSend("/topic/signal", message);
        log.debug("[WS] Sent message for lane {}", message.getLaneId());
    }

    @Override
    public void sendSignalUpdates(List<SignalMessage> messages) {
        messages.forEach(this::sendSignalUpdate);
        log.info("[WS] Sent {} messages to /topic/signal", messages.size());
    }

    @Override
    public void sendTrafficLog(TrafficLogResponse trafficLog) {
        messagingTemplate.convertAndSend("/topic/traffic-logs", trafficLog);
        log.debug("[WS] Sent traffic-log for laneId={}, congestion={}%",
                trafficLog.getLaneId(), trafficLog.getCongestionLevel());
    }

    @Override
    public void sendTrafficLogs(List<TrafficLogResponse> logs) {
        messagingTemplate.convertAndSend("/topic/traffic-logs", logs);
        log.info("[WS] Sent {} traffic-log entries to /topic/traffic-logs", logs.size());
    }
}
