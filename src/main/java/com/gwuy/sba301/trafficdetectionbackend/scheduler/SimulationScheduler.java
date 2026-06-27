package com.gwuy.sba301.trafficdetectionbackend.scheduler;

import com.gwuy.sba301.trafficdetectionbackend.service.impls.AdaptiveSignalServiceImpl;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.IntersectionDecision;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficDecisionMessage;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficInput;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.AdaptiveSignalService;
import com.gwuy.sba301.trafficdetectionbackend.websocket.TrafficPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Scheduler component that simulates traffic data generation and decision publishing.
 */
@Slf4j
@Component
public class SimulationScheduler {

    private final AdaptiveSignalService adaptiveSignalServiceImpl;
    private final TrafficPublisher trafficPublisher;
    private final Random random = new Random();

    /**
     * Constructor for SimulationScheduler.
     *
     * @param adaptiveSignalServiceImpl the service used to calculate signal timings
     * @param trafficPublisher the publisher used to send updates via WebSocket
     */
    public SimulationScheduler(AdaptiveSignalServiceImpl adaptiveSignalServiceImpl, TrafficPublisher trafficPublisher) {
        this.adaptiveSignalServiceImpl = adaptiveSignalServiceImpl;
        this.trafficPublisher = trafficPublisher;
    }

    /**
     * Simulates traffic every 5 seconds.
     * Generates random traffic data for North-South and East-West lanes.
     * Calculates synchronized signal decisions and publishes them via WebSocket.
     */
    //@Scheduled(fixedDelay = 5000)
    public void simulateTraffic() {
        // Generate random traffic values (0-100)
        // Congestion level rounded to 1 decimal place
        TrafficInput northSouth = new TrafficInput(
                "North-South",
                random.nextInt(101),
                Math.round(random.nextDouble() * 1000) / 10.0
        );

        TrafficInput eastWest = new TrafficInput(
                "East-West",
                random.nextInt(101),
                Math.round(random.nextDouble() * 1000) / 10.0
        );

        log.info("Generated traffic values: NS congestion={}, EW congestion={}",
                northSouth.getCongestionLevel(), eastWest.getCongestionLevel());

        // Calculate synchronized intersection decision
        IntersectionDecision decision = adaptiveSignalServiceImpl.calculateIntersectionDecision(northSouth, eastWest);

        log.info("Final AI decision: NS green={} red={}, EW green={} red={}",
                decision.getNorthSouthDecision().getGreenDuration(),
                decision.getNorthSouthDecision().getRedDuration(),
                decision.getEastWestDecision().getGreenDuration(),
                decision.getEastWestDecision().getRedDuration());

        // Create and publish the real-time message
        TrafficDecisionMessage message = new TrafficDecisionMessage(
                northSouth,
                eastWest,
                decision,
                LocalDateTime.now()
        );

        trafficPublisher.publishDecision(message);
    }
}
