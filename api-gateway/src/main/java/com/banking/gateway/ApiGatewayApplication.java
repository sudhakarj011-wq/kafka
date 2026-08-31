package com.banking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — Single Entry Point for All Banking APIs
 *
 * ======================== API GATEWAY ROLE ========================
 * Without Gateway:               With Gateway:
 *
 * Angular → :8081 Account        Angular
 * Angular → :8082 Payment          ↓
 * Angular → :8083 Notification   Gateway (:8080)
 * Angular → :8086 Analytics        ├── :8081 Account
 *                                   ├── :8082 Payment
 * Multiple URLs!                    ├── :8083 Notification
 * CORS on every service!            └── :8086 Analytics
 * JWT validated everywhere!
 *                                 One URL! One CORS! One JWT check!
 *
 * Gateway responsibilities:
 * 1. Route requests to correct microservice
 * 2. Validate JWT token ONCE (not repeated in every service)
 * 3. Handle CORS globally for Angular
 * 4. Load balance across multiple service instances (lb://)
 * 5. Add X-User-Id header for downstream services
 *
 * IMPORTANT: API Gateway uses Spring WebFlux (reactive/non-blocking).
 * Do NOT add spring-boot-starter-web — it conflicts!
 *
 * INTERVIEW TIP:
 * Q: Why JWT in Gateway, not in each service?
 * A: DRY — Don't Repeat Yourself. If you validate JWT in each
 *    service, you need to share the secret key with all services
 *    and maintain the same validation logic everywhere. In the
 *    Gateway, validation happens ONCE. Downstream services trust
 *    any request that passes through the Gateway (internal network).
 *    In production, use mTLS between Gateway and services.
 * ==================================================================
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("================================================");
        System.out.println("  API Gateway started on port 8080              ");
        System.out.println("  Routes: /api/accounts → account-service        ");
        System.out.println("          /api/payments → payment-service         ");
        System.out.println("          /api/notifications → notification-svc   ");
        System.out.println("          /api/analytics → analytics-service      ");
        System.out.println("================================================");
    }
}
