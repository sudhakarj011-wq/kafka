package com.banking.payment.client;

import com.banking.payment.dto.AccountClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Account Service Feign Client — Declarative REST Client
 *
 * ======================== OPENFEIGN EXPLAINED ========================
 * OpenFeign creates a proxy implementation of this interface at startup.
 * When you call accountServiceClient.getAccount("ACC1001"), Feign:
 * 1. Builds the HTTP request: GET http://account-service/api/accounts/ACC1001
 * 2. Sends the request (using Eureka to resolve "account-service" URL)
 * 3. Deserializes the response JSON → AccountResponse object
 * 4. Returns the object
 *
 * NO RestTemplate, NO HttpClient boilerplate!
 *
 * URL RESOLUTION:
 * - name="account-service" → Feign looks up this service in Eureka
 * - Eureka returns: http://192.168.1.100:8081
 * - Feign calls: http://192.168.1.100:8081/api/accounts/ACC1001
 *
 * For Phase 2 (no Eureka), we use url="${account-service.url}" from yml.
 * For Phase 3+, Eureka resolves the URL by service name automatically.
 *
 * INTERVIEW TIP:
 * Q: OpenFeign vs RestTemplate vs WebClient?
 * A: RestTemplate — old, verbose, lots of boilerplate
 *    WebClient — reactive, non-blocking, complex syntax
 *    OpenFeign — declarative, clean, synchronous, best for microservices
 *    Use WebClient only when you need reactive programming (Spring WebFlux).
 * =====================================================================
 */
@FeignClient(
    name = "account-service",          // Eureka service name (Phase 3+)
    url = "${account-service.url:}"    // Direct URL fallback (Phase 2)
)
public interface AccountServiceClient {

    /**
     * Get account details — used to validate accounts exist before transfer.
     * GET http://account-service/api/accounts/{accountNumber}
     */
    @GetMapping("/api/accounts/{accountNumber}")
    AccountClientDto.AccountResponse getAccount(@PathVariable String accountNumber);

    /**
     * Debit (subtract) from sender's account.
     * PUT http://account-service/api/accounts/{accountNumber}/debit
     *
     * Called BY Payment Service INSIDE @Transactional block.
     * If this call fails → entire transfer rolls back (both DB + Kafka).
     */
    @PutMapping("/api/accounts/{accountNumber}/debit")
    AccountClientDto.AccountResponse debitAccount(
            @PathVariable String accountNumber,
            @RequestBody AccountClientDto.BalanceUpdateRequest request);

    /**
     * Credit (add) to receiver's account.
     * PUT http://account-service/api/accounts/{accountNumber}/credit
     */
    @PutMapping("/api/accounts/{accountNumber}/credit")
    AccountClientDto.AccountResponse creditAccount(
            @PathVariable String accountNumber,
            @RequestBody AccountClientDto.BalanceUpdateRequest request);
}
