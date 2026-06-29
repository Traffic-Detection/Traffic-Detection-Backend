package com.gwuy.sba301.trafficdetectionbackend.entity;

import com.gwuy.sba301.trafficdetectionbackend.enums.LaneStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "lanes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Quan hệ Many-To-One với Intersection
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intersection_id", nullable = false)
    Intersection intersection;

    @Column(name = "lane_name")
    String laneName;

    @Column(name = "direction_name", nullable = false, length = 100)
    String directionName;

    @Column(name = "movement")
    String movement;

    @Column(name = "lane_order")
    String laneOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    LaneStatus status;

    @Column(name = "created_at")
    Long createdAt;

    @Column(name = "updated_at")
    Long updatedAt;

    // Tự tham chiếu (Self-referencing) đến làn đối diện
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opposing_lane_id")
    Lane opposingLane;
}
