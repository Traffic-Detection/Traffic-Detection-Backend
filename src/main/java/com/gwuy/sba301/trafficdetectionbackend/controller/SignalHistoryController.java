package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalHistoryResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Signal History API")
@RestController
@CrossOrigin("*")
@RequestMapping("/api/signal-history")
@RequiredArgsConstructor
public class SignalHistoryController {

    private final SignalHistoryService signalHistoryService;

    @Operation(summary = "Get all signal history")
    @GetMapping
    public ResponseEntity<List<SignalHistoryResponse>> getAllSignalHistory() {
        return ResponseEntity.ok(signalHistoryService.getAllSignalHistory());
    }
}
