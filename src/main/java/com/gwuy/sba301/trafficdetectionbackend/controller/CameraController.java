package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.CameraResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.TrafficControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Camera API")
@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final TrafficControlService trafficControlService;

    @Operation(summary = "Get list of all cameras")
    @GetMapping
    public ResponseEntity<List<CameraResponse>> getAllCameras() {
        return ResponseEntity.ok(trafficControlService.getAllCameras());
    }
}