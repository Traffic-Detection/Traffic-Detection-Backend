package com.gwuy.sba301.trafficdetectionbackend.controller;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.SignalConfigRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.SignalConfigResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.interfaces.SignalConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Signal Config API", description = "Quản lý cấu hình thời lượng đèn cho chế độ MANUAL")
@RestController
@RequestMapping("/api/signal-configs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SignalConfigController {

    private final SignalConfigService signalConfigService;

    @Operation(summary = "Lấy danh sách cấu hình đèn theo ngã tư")
    @GetMapping
    public ResponseEntity<List<SignalConfigResponse>> getConfigs(@RequestParam Long intersectionId) {
        return ResponseEntity.ok(signalConfigService.getConfigsByIntersection(intersectionId));
    }

    @Operation(summary = "Tạo cấu hình đèn mới cho một làn đường")
    @PostMapping
    public ResponseEntity<SignalConfigResponse> createConfig(@Valid @RequestBody SignalConfigRequest request) {
        return ResponseEntity.ok(signalConfigService.createConfig(request));
    }

    @Operation(summary = "Cập nhật thời lượng đèn")
    @PutMapping("/{id}")
    public ResponseEntity<SignalConfigResponse> updateConfig(@PathVariable Long id, @Valid @RequestBody SignalConfigRequest request) {
        return ResponseEntity.ok(signalConfigService.updateConfig(id, request));
    }

    @Operation(summary = "Xoá cấu hình đèn")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        signalConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }
}