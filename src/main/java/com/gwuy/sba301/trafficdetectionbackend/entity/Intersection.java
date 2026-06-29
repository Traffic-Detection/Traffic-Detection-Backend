package com.gwuy.sba301.trafficdetectionbackend.entity;

import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import com.gwuy.sba301.trafficdetectionbackend.enums.IntersectionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "intersections", uniqueConstraints = @UniqueConstraint(name = "uk_intersections_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Intersection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "address")
    String address;

    @Column(name = "coordinates", columnDefinition = "json")
    String coordinates;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    IntersectionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_mode", nullable = false, length = 30)
    OperatingMode operatingMode;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    Long updatedAt;
}