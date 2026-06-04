package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signal_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intersection_id", nullable = false)
    private Intersection intersection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @Column(name = "green_duration", nullable = false)
    private Integer greenDuration;

    @Column(name = "red_duration", nullable = false)
    private Integer redDuration;

    @Column(name = "applied_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime appliedAt;
}
