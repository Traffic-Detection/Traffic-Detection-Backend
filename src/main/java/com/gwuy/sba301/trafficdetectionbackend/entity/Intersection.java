package com.gwuy.sba301.trafficdetectionbackend.entity;

import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "intersections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intersection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_mode", nullable = false, length = 30)
    private OperatingMode operatingMode;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}