package com.gwuy.sba301.trafficdetectionbackend.exception;

/**
 * Thrown when the OSRM routing service is unreachable
 * or returns an unexpected error response.
 */
public class OsrmServiceException extends RuntimeException {

    public OsrmServiceException(String message) {
        super(message);
    }

    public OsrmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
