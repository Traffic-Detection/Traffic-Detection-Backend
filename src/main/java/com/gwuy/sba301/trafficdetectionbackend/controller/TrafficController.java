package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.response.CurrentTrafficResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.TrafficControlService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Traffic API", description = "Cung cấp dữ liệu bản đồ Real-time từ AI")
@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrafficController {

    private final TrafficControlService trafficControlService;

    @GetMapping("/current")
    public ResponseEntity<CurrentTrafficResponse> getCurrentTraffic() {
        return ResponseEntity.ok(trafficControlService.getCurrentTraffic());
    }
}