package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.service.TrafficControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; // <-- Import thêm thư viện này
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Traffic Log API")
@RestController
@RequestMapping("/api/traffic-logs")
@RequiredArgsConstructor
public class TrafficLogController {

    private final TrafficControlService trafficControlService;

    @Operation(summary = "Record traffic density log from Camera AI")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request format"),
            @ApiResponse(responseCode = "404", description = "Lane not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Void> recordTrafficLog(@Valid @RequestBody TrafficLogRequest request) {
        trafficControlService.recordTrafficLog(request);
        return ResponseEntity.ok().build();
    }
}