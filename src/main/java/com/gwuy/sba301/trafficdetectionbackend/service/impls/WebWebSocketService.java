package com.gwuy.sba301.trafficdetectionbackend.service.impls;

import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficLogResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.IWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebWebSocketService implements IWebSocketService {

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

    /**
     * Broadcast a new traffic log entry to all subscribers of /topic/traffic-logs.
     * Called every time a new log is recorded via POST /api/traffic-logs.
     */
    @Override
    public void sendTrafficLog(TrafficLogResponse trafficLog) {
        messagingTemplate.convertAndSend("/topic/traffic-logs", trafficLog);
        log.debug("[WS] Sent traffic-log for laneId={}, congestion={}%",
                trafficLog.getLaneId(), trafficLog.getCongestionLevel());
    }

    /**
     * Broadcast a list of traffic logs (e.g. on initial connection snapshot).
     */
    @Override
    public void sendTrafficLogs(List<TrafficLogResponse> logs) {
        messagingTemplate.convertAndSend("/topic/traffic-logs", logs);
        log.info("[WS] Sent {} traffic-log entries to /topic/traffic-logs", logs.size());
    }
}
