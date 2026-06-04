package com.gwuy.sba301.trafficdetectionbackend.entity;


import com.gwuy.sba301.trafficdetectionbackend.enums.CameraStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "camera_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @Column(name = "ip_address", nullable = false, unique = true, length = 100)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CameraStatus status;
}
