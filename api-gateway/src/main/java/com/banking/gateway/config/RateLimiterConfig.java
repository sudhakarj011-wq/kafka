package com.banking.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Configuration for API Gateway
 *
 * INTERVIEW TIP — Kaise kaam karta hai Rate Limiting?
 * =====================================================
 * Spring Cloud Gateway ka built-in `RequestRateLimiter` filter,
 * Redis mein ek sliding window maintain karta hai per key (IP/user).
 *
 * Flow:
 * 1. Request aati hai
 * 2. KeyResolver run hota hai → unique key decide karta hai (IP address ya customerId)
 * 3. Redis mein check hota hai: is key ne last 1 second mein kitni requests ki?
 * 4. Agar limit exceed → 429 Too Many Requests return
 * 5. Agar allowed → request forward hoti hai
 *
 * Configuration in application.yml:
 *   redis-rate-limiter.replenishRate: 5  → 5 tokens/second add hote hain
 *   redis-rate-limiter.burstCapacity: 10 → max 10 tokens store ho sakte hain
 *
 * INTERVIEW TIP — Why Redis for Rate Limiting?
 * Agar hum memory mein count rakhein, toh each service instance ka
 * apna count hoga. 3 instances = 3x allowed requests (wrong!).
 * Redis centralized hai — saare instances ek hi counter share karte hain.
 *
 * INTERVIEW TIP — Ye Gateway-level rate limiting Security ke liye hai.
 * Business-level throttling (e.g., ek account se daily 10 transfers max)
 * Payment Service ke andar honi chahiye.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * IP-based Key Resolver.
     * Har unique IP address ko alag rate limiting bucket milta hai.
     *
     * Use Case: Ek attacker ka IP address 429 ban kar deta hai,
     * baki users ki normal service continue rahti hai.
     *
     * INTERVIEW TIP — Alternative: User-based resolver
     * Agar JWT validated hai, toh X-Customer-Id header se customerId
     * nikal ke per-customer rate limiting bhi implement kar sakte hain.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just(clientIp);
        };
    }

    /**
     * Customer ID-based Key Resolver (alternative to IP-based).
     * Uses the X-Customer-Id header injected by JwtAuthFilter after token validation.
     *
     * Usage: Set key-resolver: "#{@customerKeyResolver}" in application.yml
     */
    @Bean
    public KeyResolver customerKeyResolver() {
        return exchange -> {
            String customerId = exchange.getRequest().getHeaders()
                    .getFirst("X-Customer-Id");
            return Mono.just(customerId != null ? customerId : "anonymous");
        };
    }
}
