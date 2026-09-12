package com.banking.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Idempotency Filter for API Gateway.
 *
 * This filter prevents duplicate transactions (e.g. Double Charging)
 * if a client accidentally clicks "Transfer" twice or if a network retry happens.
 *
 * It intercepts POST requests, requires an "X-Idempotency-Key" header from the frontend,
 * and checks Redis to see if that key was already processed recently.
 */
@Component
public class IdempotencyFilter extends AbstractGatewayFilterFactory<IdempotencyFilter.Config> {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    public IdempotencyFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Only apply Idempotency to POST requests (modifying state)
            if (exchange.getRequest().getMethod() != HttpMethod.POST) {
                return chain.filter(exchange);
            }

            String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");

            // For financial transactions, strict idempotency is mandatory
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                return onError(exchange, "Missing X-Idempotency-Key header for POST request", HttpStatus.BAD_REQUEST);
            }

            String redisKey = "idempotency:" + idempotencyKey;

            // setIfAbsent equivalent to Redis SETNX (Set if Not eXists)
            // It is an atomic operation returning true if the key was set, false if it already existed.
            return redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24))
                    .flatMap(isNewRequest -> {
                        if (Boolean.TRUE.equals(isNewRequest)) {
                            // Key didn't exist, proceed with request
                            return chain.filter(exchange);
                        } else {
                            // Key already exists (duplicate request)
                            return onError(exchange, "Duplicate request detected. Transaction already processed.", HttpStatus.CONFLICT);
                        }
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        // Add specific header so frontend can detect idempotency failures easily
        exchange.getResponse().getHeaders().add("X-Error-Message", err);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if any
    }
}
