package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {
    private Long intersectionId;
    private Long laneId;
    private String direction;
    private String signal; // GREEN, RED
    private Integer greenDuration;
    private Integer redDuration;
    private Integer remaining;
    private String trafficLevel; // LOW, MEDIUM, HIGH
}
