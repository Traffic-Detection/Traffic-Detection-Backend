package com.gwuy.sba301.trafficdetectionbackend.exception;

public class IntersectionNotFoundException extends RuntimeException {
    public IntersectionNotFoundException(Long id) {
        super("Intersection not found with id: " + id);
    }
}
