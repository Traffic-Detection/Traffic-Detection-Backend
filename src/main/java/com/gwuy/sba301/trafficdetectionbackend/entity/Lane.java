package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lanes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ Many-To-One với Intersection
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intersection_id", nullable = false)
    private Intersection intersection;

    @Column(name = "direction_name", nullable = false, length = 100)
    private String directionName;

    // Tự tham chiếu (Self-referencing) đến làn đối diện
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opposing_lane_id")
    private Lane opposingLane;
}
