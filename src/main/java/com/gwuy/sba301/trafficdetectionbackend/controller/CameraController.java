package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.CameraCreateRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.CameraResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Camera API")
@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CameraController {

    private final TrafficControlService trafficControlService;

    @Operation(summary = "Get list of all cameras")
    @GetMapping
    public ResponseEntity<List<CameraResponse>> getAllCameras() {
        return ResponseEntity.ok(trafficControlService.getAllCameras());
    }

    @Operation(summary = "Create a new camera for a lane")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Camera created successfully"),
            @ApiResponse(responseCode = "404", description = "Lane not found")
    })
    @PostMapping("/lanes/{laneId}")
    public ResponseEntity<CameraResponse> createCamera(@PathVariable Long laneId, @RequestBody CameraCreateRequest request) {
        CameraResponse response = trafficControlService.createCamera(laneId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }
}