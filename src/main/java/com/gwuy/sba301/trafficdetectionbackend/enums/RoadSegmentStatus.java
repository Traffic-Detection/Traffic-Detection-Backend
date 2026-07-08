package com.gwuy.sba301.trafficdetectionbackend.enums;

/**
 * Represents the operational status of a road segment.
 *
 * <ul>
 *   <li>{@code ACTIVE} — The road segment is in use and included in route calculations</li>
 *   <li>{@code INACTIVE} — The road segment is disabled (e.g., under construction)</li>
 * </ul>
 */
public enum RoadSegmentStatus {
    ACTIVE,
    INACTIVE
}
