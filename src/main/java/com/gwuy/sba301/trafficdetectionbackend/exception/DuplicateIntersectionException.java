package com.gwuy.sba301.trafficdetectionbackend.exception;

public class DuplicateIntersectionException extends RuntimeException {
    public DuplicateIntersectionException(String name) {
        super("Intersection already exists: " + name);
    }
}
