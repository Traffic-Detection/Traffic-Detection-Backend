package com.gwuy.sba301.trafficdetectionbackend.dto.request;

import com.gwuy.sba301.trafficdetectionbackend.enums.LaneStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LaneCreateRequest {
    String laneName;
    String directionName;
    String movement;
    String laneOrder;
    LaneStatus status;
}
