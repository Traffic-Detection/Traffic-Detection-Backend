package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persists route recommendation history.
 * Stores the start/end coordinates, selected route index, scoring results,
 * and the GeoJSON geometry of the recommended route.
 */
@Entity
@Table(name = "route_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "start_lat", nullable = false)
    Double startLat;

    @Column(name = "start_lng", nullable = false)
    Double startLng;

    @Column(name = "end_lat", nullable = false)
    Double endLat;

    @Column(name = "end_lng", nullable = false)
    Double endLng;

    @Column(name = "selected_route_index", nullable = false)
    Integer selectedRouteIndex;

    @Column(name = "total_routes", nullable = false)
    Integer totalRoutes;

    @Column(name = "total_score", nullable = false)
    Double totalScore;

    @Column(name = "total_distance")
    Double totalDistance;

    @Column(name = "total_duration")
    Double totalDuration;

    @Column(name = "route_geometry", columnDefinition = "TEXT")
    String routeGeometry;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;
}
