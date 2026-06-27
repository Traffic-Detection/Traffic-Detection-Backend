package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.LoginRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.RegisterRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.TokenRefreshRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
}
