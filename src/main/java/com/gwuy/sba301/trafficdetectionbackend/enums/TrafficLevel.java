package com.gwuy.sba301.trafficdetectionbackend.enums;

/**
 * Represents the traffic congestion level at an intersection or road segment.
 * Used by the Route Recommendation algorithm to calculate traffic penalties.
 *
 * <ul>
 *   <li>{@code LOW} — Free-flowing traffic</li>
 *   <li>{@code MEDIUM} — Moderate congestion</li>
 *   <li>{@code HIGH} — Heavy congestion</li>
 * </ul>
 */
public enum TrafficLevel {
    LOW,
    MEDIUM,
    HIGH
}
