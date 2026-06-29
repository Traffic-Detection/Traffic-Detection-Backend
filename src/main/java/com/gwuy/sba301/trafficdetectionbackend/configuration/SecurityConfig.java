package com.gwuy.sba301.trafficdetectionbackend.configuration;

import com.gwuy.sba301.trafficdetectionbackend.service.impls.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    // Inject thêm bộ lọc JWT mà bạn đã tạo
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC: Mở cửa cho Đăng nhập, Đăng ký và kết nối WebSocket
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh-token").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/traffic-ws/**").permitAll()

                        // 2. CAMERA AI: Chỉ dành riêng cho thiết bị phần cứng đẩy dữ liệu kẹt xe lên
                        .requestMatchers(HttpMethod.POST, "/api/traffic-logs").hasAuthority("ROLE_CAMERA")

                        // 3. ADMIN & OPERATOR: Được phép cấu hình đèn và chuyển chế độ (MANUAL/AI)
                        .requestMatchers(HttpMethod.PUT, "/api/intersections/*/operating-mode").hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/signal-configs").hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/signal-configs/*").hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/signal-configs/*").hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")

                        // 4. VIEWER: Mọi tài khoản đăng nhập đều có quyền GET (xem dữ liệu báo cáo)
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

                        // Khoá tất cả các API còn lại
                        .anyRequest().authenticated()
                )
                // Cấu hình không lưu Session ở Backend (vì dùng JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Đăng ký Provider và đưa JwtFilter vào luồng kiểm tra
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("[DEBUG_LOG] Auth exception: " + authException.getMessage() + " for path: " + request.getServletPath());
                            response.sendError(401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            System.out.println("[DEBUG_LOG] Access denied: " + accessDeniedException.getMessage() + " for path: " + request.getServletPath());
                            response.sendError(403, "Forbidden");
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}