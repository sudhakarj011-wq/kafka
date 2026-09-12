package com.banking.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for Account Service.
 *
 * INTERVIEW TIP - Defense in Depth:
 * Even though API Gateway validates the JWT, individual microservices should
 * also have security (Zero Trust Architecture).
 *
 * For now, we allow internal traffic, but in production, we would validate
 * an internal mesh token (e.g., mTLS or intra-service JWT) here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // /api/accounts/** = direct calls from other services (gRPC fallback, internal)
                        // /api/v1/accounts/** = calls routed through API Gateway
                        .requestMatchers("/api/accounts/**", "/api/v1/accounts/**").permitAll()
                        .anyRequest().denyAll()  // INTERVIEW TIP: deny-by-default. Explicit allow is safer.
                )
                .build();
    }
}
