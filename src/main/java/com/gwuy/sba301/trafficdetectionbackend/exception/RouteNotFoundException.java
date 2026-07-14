package com.gwuy.sba301.trafficdetectionbackend.exception;

/**
 * Thrown when no routes are found by the OSRM service
 * for the given start and end coordinates.
 */
public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String message) {
        super(message);
    }
}
