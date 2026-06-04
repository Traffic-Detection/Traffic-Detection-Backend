package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import com.gwuy.sba301.trafficdetectionbackend.enums.OperatingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntersectionResponse {

    private Long id;
    private String name;
    private OperatingMode operatingMode;
    private LocalDateTime createdAt;
}
