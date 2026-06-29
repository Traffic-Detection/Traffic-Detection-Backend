package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SignalHistoryResponse {
    private Long id;
    private Long intersectionId;
    private Long laneId;
    private Integer greenDuration;
    private Integer yellowDuration;
    private Integer redDuration;
    private LocalDateTime appliedAt;
}