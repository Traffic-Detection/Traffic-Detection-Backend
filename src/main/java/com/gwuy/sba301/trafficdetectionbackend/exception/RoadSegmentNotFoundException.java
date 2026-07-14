package com.gwuy.sba301.trafficdetectionbackend.exception;

/**
 * Thrown when a road segment is not found by its ID.
 */
public class RoadSegmentNotFoundException extends RuntimeException {

    public RoadSegmentNotFoundException(String message) {
        super(message);
    }
}
