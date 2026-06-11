package com.gwuy.sba301.trafficdetectionbackend.atcs.websocket;

import com.gwuy.sba301.trafficdetectionbackend.atcs.model.TrafficDecisionMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher service for sending traffic updates over WebSocket.
 */
@Component
public class TrafficPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Constructor for TrafficPublisher.
     *
     * @param messagingTemplate Spring's messaging template for WebSocket communication
     */
    public TrafficPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes the given TrafficDecisionMessage to the /topic/traffic destination.
     * 
     * @param message the traffic decision message to publish
     */
    public void publishDecision(TrafficDecisionMessage message) {
        messagingTemplate.convertAndSend("/topic/traffic", message);
    }
}
