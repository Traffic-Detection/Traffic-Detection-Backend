package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.UpdateOperatingModeRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.IntersectionResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.LaneResponse;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.TrafficControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Intersection API")
@RestController
@RequestMapping("/api/intersections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IntersectionController {

    private final TrafficControlService trafficControlService;

    @Operation(summary = "Update intersection operating mode (Manual Override)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mode updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Intersection not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/operating-mode")
    public ResponseEntity<IntersectionResponse> updateOperatingMode(
            @PathVariable Long id,
            @RequestBody UpdateOperatingModeRequest request) {

        IntersectionResponse response = trafficControlService.updateOperatingMode(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger adaptive signal processing (For testing/Cron trigger)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processing completed"),
            @ApiResponse(responseCode = "404", description = "Intersection not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/adaptive-signals")
    public ResponseEntity<Void> processAdaptiveSignals(@PathVariable("id") Long id) {

        trafficControlService.processAdaptiveSignals(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get list of all intersections")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<IntersectionResponse>> getAllIntersections() {
        List<IntersectionResponse> response = trafficControlService.getAllIntersections();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get lanes by intersection ID")
    @GetMapping("/{id}/lanes")
    public ResponseEntity<List<LaneResponse>> getLanesByIntersection(@PathVariable Long id) {
        return ResponseEntity.ok(trafficControlService.getLanesByIntersection(id));
    }

    @Operation(summary = "Get signal history by intersection ID")
    @GetMapping("/{id}/signal-history")
    public ResponseEntity<List<SignalHistoryResponse>> getSignalHistory(@PathVariable Long id) {
        return ResponseEntity.ok(trafficControlService.getSignalHistoryByIntersection(id));
    }
}
