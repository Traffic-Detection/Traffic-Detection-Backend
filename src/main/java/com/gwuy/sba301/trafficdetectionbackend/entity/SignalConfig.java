package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "signal_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intersection_id", nullable = false)
    Intersection intersection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    Lane lane;

    @Column(name = "yellow_duration", nullable = false)
    Integer yellowDuration;

    @Column(name = "green_duration", nullable = false)
    Integer greenDuration;

    @Column(name = "red_duration", nullable = false)
    Integer redDuration;
}
