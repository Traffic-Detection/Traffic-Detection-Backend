package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.dto.SignalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendSignalUpdate(SignalMessage message) {
        messagingTemplate.convertAndSend("/topic/signal", message);
        log.debug("[WS] Sent message for lane {}", message.getLaneId());
    }

    public void sendSignalUpdates(List<SignalMessage> messages) {
        messages.forEach(this::sendSignalUpdate);
        log.info("[WS] Sent {} messages to /topic/signal", messages.size());
    }
}
