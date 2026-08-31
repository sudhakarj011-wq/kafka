package com.banking.account.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Auth Controller — handles Login and JWT Generation
 *
 * This endpoint is PUBLIC (routes through Gateway without JWT check)
 * Path: /api/auth/login
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    // MUST match the secret in API Gateway application.yml
    private final String secret = "banking-payment-system-super-secret-key-2026-do-not-share-in-production";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login request for customer: {}", request.getCustomerId());

        // In a real app, validate password against DB here.
        // For this demo, we assume any customerId/password is valid if not empty.
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Customer ID required"));
        }

        String token = generateToken(request.getCustomerId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "customerId", request.getCustomerId(),
                "message", "Login successful"
        ));
    }

    private String generateToken(String customerId) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long expirationTime = 86400000; // 24 hours

        return Jwts.builder()
                .subject(customerId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    @Data
    public static class LoginRequest {
        private String customerId;
        private String password;
    }
}
