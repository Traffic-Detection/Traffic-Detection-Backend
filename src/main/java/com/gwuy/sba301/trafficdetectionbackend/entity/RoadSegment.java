package com.gwuy.sba301.trafficdetectionbackend.entity;

import com.gwuy.sba301.trafficdetectionbackend.enums.RoadSegmentStatus;
import com.gwuy.sba301.trafficdetectionbackend.enums.TrafficLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a road segment connecting two intersections.
 * Used by the Route Recommendation algorithm to evaluate traffic conditions
 * and calculate route penalties.
 *
 * <p>Each road segment has a traffic level (LOW, MEDIUM, HIGH) that is
 * updated either by the traffic simulation or by real YOLO data.</p>
 */
@Entity
@Table(name = "road_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "road_name", nullable = false)
    String roadName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_intersection_id", nullable = false)
    Intersection fromIntersection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_intersection_id", nullable = false)
    Intersection toIntersection;

    @Column(name = "distance", nullable = false)
    Double distance;

    @Column(name = "speed_limit")
    Integer speedLimit;

    @Column(name = "osm_way_id")
    Long osmWayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_level", nullable = false, length = 20)
    TrafficLevel trafficLevel;

    @Column(name = "traffic_cost")
    Double trafficCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    RoadSegmentStatus status;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}
