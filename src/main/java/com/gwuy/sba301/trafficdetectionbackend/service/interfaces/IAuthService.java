package com.gwuy.sba301.trafficdetectionbackend.service.interfaces;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.LoginRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.RegisterRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.TokenRefreshRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.auth.AuthResponse;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
}
