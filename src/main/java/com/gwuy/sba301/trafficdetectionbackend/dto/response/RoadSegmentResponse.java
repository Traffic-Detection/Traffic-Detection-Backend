package com.gwuy.sba301.trafficdetectionbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing a road segment with its current traffic information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadSegmentResponse {

    private Long id;
    private String roadName;
    private Long fromIntersectionId;
    private String fromIntersectionName;
    private Long toIntersectionId;
    private String toIntersectionName;
    private Double distance;
    private Integer speedLimit;
    private Long osmWayId;
    private String trafficLevel;
    private Double trafficCost;
    private String status;
}
