package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.CurrentTrafficResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SimulationStatusResponse;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;

/**
 * Service interface for traffic simulation management.
 * Controls the mock traffic data generation used by the Route Recommendation prototype.
 *
 * <p>The simulation randomly generates vehicle counts every 5 seconds,
 * derives traffic levels, and updates intersections and road segments.</p>
 */
public interface TrafficSimulationService {

    /**
     * Start the traffic simulation. Generates random vehicle counts every 5 seconds.
     *
     * @return status indicating whether the simulation was started
     */
    SimulationStatusResponse startSimulation();

    /**
     * Stop the traffic simulation.
     *
     * @return status indicating whether the simulation was stopped
     */
    SimulationStatusResponse stopSimulation();

    /**
     * Get the current traffic status across all intersections and road segments.
     *
     * @return aggregate traffic data
     */
    CurrentTrafficResponse getCurrentTraffic();

    /**
     * Check whether the simulation is currently running.
     *
     * @return true if simulation is active
     */
    boolean isRunning();

    /**
     * Get the current traffic level for a specific intersection.
     * Returns the simulated traffic level from the in-memory cache.
     *
     * @param intersectionId the intersection ID
     * @return the current traffic level (defaults to LOW if not simulated)
     */
    TrafficLevel getIntersectionTrafficLevel(Long intersectionId);
}
