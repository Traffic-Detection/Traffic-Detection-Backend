package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.TrafficLogRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.TrafficLogResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.TrafficControlService;
import com.gwuy.sba301.trafficdetectionbackend.service.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Traffic Log API")
@RestController
@RequestMapping("/api/traffic-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrafficLogController {

    private final TrafficControlService trafficControlService;
    private final WebSocketService webSocketService;

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

    @Operation(summary = "Get all traffic logs (REST - use for initial load; subscribe WebSocket /topic/traffic-logs for real-time updates)")
    @GetMapping
    public ResponseEntity<List<TrafficLogResponse>> getAllTrafficLogs() {
        return ResponseEntity.ok(trafficControlService.getAllTrafficLogs());
    }

    /**
     * WebSocket handler: client sends any message to /app/traffic-logs
     * and receives the full current snapshot broadcast to /topic/traffic-logs.
     *
     * Usage (STOMP client):
     *   stompClient.subscribe('/topic/traffic-logs', callback);
     *   stompClient.send('/app/traffic-logs', {}, '');  // trigger snapshot
     */
    @MessageMapping("/traffic-logs")
    public void sendTrafficLogsSnapshot() {
        List<TrafficLogResponse> logs = trafficControlService.getAllTrafficLogs();
        webSocketService.sendTrafficLogs(logs);
    }
}
