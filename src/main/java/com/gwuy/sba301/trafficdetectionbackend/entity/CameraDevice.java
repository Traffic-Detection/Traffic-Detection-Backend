package com.gwuy.sba301.trafficdetectionbackend.entity;

import com.gwuy.sba301.trafficdetectionbackend.enums.CameraStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "camera_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "camera_code")
    String cameraCode;

    @Column(name = "camera_name")
    String cameraName;

    @Column(name = "ip_address", nullable = false, unique = true, length = 100)
    String ipAddress;

    @Column(name = "mac_address")
    String macAddress;

    @Column(name = "serial_number")
    String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    Lane lane;

    @Column(name = "model")
    String model;

    @Column(name = "manufacturer")
    String manufacturer;

    @Column(name = "operating_status")
    String operatingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    CameraStatus status;

    @Column(name = "create_at")
    Long createAt;

    @Column(name = "updated_at")
    Long updatedAt;
}
