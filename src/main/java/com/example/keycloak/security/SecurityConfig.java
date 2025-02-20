package com.example.keycloak.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.access.AccessDeniedHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter; // ✅ 確保 JwtAuthConverter 解析角色

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 設定為無狀態 API（RESTful API）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 啟用 CORS
            .cors(cors -> cors.configure(http))

            // 禁用 CSRF（因為使用 JWT）
            .csrf(csrf -> csrf.disable())

            // 設定 API 權限
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public").permitAll()  // 任何人可訪問
                .requestMatchers("/api/user").hasAuthority("ROLE_API_USER")  // ✅ 確保 Keycloak "api-user" 角色能匹配 `ROLE_API_USER`
                .requestMatchers("/api/admin").hasAuthority("ROLE_ADMIN")  // ✅ ADMIN 角色檢查
                .anyRequest().authenticated() // 其他 API 需要身份驗證
            )

            // OAuth 2.0 JWT 設定
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> 
                jwt.jwtAuthenticationConverter(jwtAuthConverter))  // ✅ 轉換 Keycloak 角色格式
            )

            // 自訂錯誤處理
            .exceptionHandling(ex -> 
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 401 未授權
                  .accessDeniedHandler(accessDeniedHandler()) // 403 禁止訪問
            );

        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("無權限存取此資源");
        };
    }
}
