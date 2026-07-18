package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    Lane lane;

    @Column(name = "vehicle_count", nullable = false)
    Integer vehicleCount;

    @Column(name = "congestion_level", nullable = false)
    Double congestionLevel;

    @Column(name = "frame_url")
    String frameUrl;

    @Column(name = "recorded_at", updatable = false, nullable = false)
    private LocalDateTime recordedAt;
}