package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.CurrentTrafficResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SimulationStatusResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Traffic Simulation operations.
 *
 * <p>Provides endpoints to start/stop the traffic simulation
 * and to retrieve the current traffic status across all
 * intersections and road segments.</p>
 *
 * <p>The simulation generates random vehicle counts every 5 seconds,
 * derives traffic levels, and updates the in-memory and database state.
 * All logic is delegated to {@link TrafficSimulationService}.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SimulationController {

    private final TrafficSimulationService trafficSimulationService;

    /**
     * Start the traffic simulation.
     * Generates random vehicle counts every 5 seconds, updating
     * intersection and road segment traffic levels.
     *
     * @return simulation status response
     */
    @PostMapping("/api/simulation/start")
    public ResponseEntity<SimulationStatusResponse> startSimulation() {
        log.info("POST /api/simulation/start");
        SimulationStatusResponse response = trafficSimulationService.startSimulation();
        return ResponseEntity.ok(response);
    }

    /**
     * Stop the traffic simulation.
     *
     * @return simulation status response
     */
    @PostMapping("/api/simulation/stop")
    public ResponseEntity<SimulationStatusResponse> stopSimulation() {
        log.info("POST /api/simulation/stop");
        SimulationStatusResponse response = trafficSimulationService.stopSimulation();
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current traffic status across all intersections and road segments.
     * Includes whether the simulation is currently running.
     *
     * @return aggregate traffic data
     */
    @GetMapping("/api/traffic/current")
    public ResponseEntity<CurrentTrafficResponse> getCurrentTraffic() {
        log.debug("GET /api/traffic/current");
        CurrentTrafficResponse response = trafficSimulationService.getCurrentTraffic();
        return ResponseEntity.ok(response);
    }
}
