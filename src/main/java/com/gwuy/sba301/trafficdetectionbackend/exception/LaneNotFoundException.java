package com.gwuy.sba301.trafficdetectionbackend.exception;

public class LaneNotFoundException extends RuntimeException {
    public LaneNotFoundException(Long id) {
        super("Lane not found with id: " + id);
    }
}