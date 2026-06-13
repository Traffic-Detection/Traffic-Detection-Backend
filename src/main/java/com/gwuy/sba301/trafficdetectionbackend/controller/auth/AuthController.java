package com.gwuy.sba301.trafficdetectionbackend.controller.auth;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.LoginRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.RegisterRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.TokenRefreshRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.auth.AuthResponse;
import com.gwuy.sba301.trafficdetectionbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
