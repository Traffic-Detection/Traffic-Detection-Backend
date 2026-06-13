package com.gwuy.sba301.trafficdetectionbackend.service;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.LoginRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.RegisterRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.TokenRefreshRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.auth.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
}
