package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signal_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intersection_id", nullable = false)
    Intersection intersection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    Lane lane;

    @Column(name = "green_duration", nullable = false)
    Integer greenDuration;

    @Column(name = "yellow_duration")
    Integer yellowDuration;

    @Column(name = "red_duration", nullable = false)
    Integer redDuration;

    @Column(name = "applied_at", updatable = false)
    @CreationTimestamp
    LocalDateTime appliedAt;
}
