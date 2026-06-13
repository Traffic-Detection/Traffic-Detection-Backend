package com.gwuy.sba301.trafficdetectionbackend.service.impl;

import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.LoginRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.RegisterRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.request.auth.TokenRefreshRequest;
import com.gwuy.sba301.trafficdetectionbackend.dto.response.auth.AuthResponse;
import com.gwuy.sba301.trafficdetectionbackend.entity.RefreshToken;
import com.gwuy.sba301.trafficdetectionbackend.entity.Role;
import com.gwuy.sba301.trafficdetectionbackend.entity.User;
import com.gwuy.sba301.trafficdetectionbackend.repository.RefreshTokenRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.RoleRepository;
import com.gwuy.sba301.trafficdetectionbackend.repository.UserRepository;
import com.gwuy.sba301.trafficdetectionbackend.service.AuthService;
import com.gwuy.sba301.trafficdetectionbackend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(userRole);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return login(new LoginRequest(user.getUsername(), request.getPassword()));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshTokenStr = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshTokenStr);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .username(user.getUsername())
                .role(user.getRole().getName())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user);
                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(requestRefreshToken)
                            .username(user.getUsername())
                            .role(user.getRole().getName())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    private void saveRefreshToken(User user, String token) {
        refreshTokenRepository.deleteByUser(user);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        
        Date expiryDate = jwtService.extractExpiration(token);
        refreshToken.setExpiryDate(expiryDate.toInstant());
        refreshToken.setRevoked(false);
        
        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
