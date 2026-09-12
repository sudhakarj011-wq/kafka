package com.banking.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for Payment Service.
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
                        // Razorpay webhook: authenticated via HMAC payload signature, not JWT
                        .requestMatchers("/api/payments/webhook", "/api/v1/payments/webhook").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // /api/payments/** = internal service calls
                        // /api/v1/payments/** = Gateway-routed calls
                        .requestMatchers("/api/payments/**", "/api/v1/payments/**").permitAll()
                        .anyRequest().denyAll()  // INTERVIEW TIP: deny-by-default. Explicit allow is safer.
                )
                .build();
    }
}
