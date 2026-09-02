package com.banking.account.controller;

import com.banking.account.dto.AccountDto;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Account Controller — REST API Layer
 *
 * ===================== REST API DESIGN =====================
 * POST   /api/accounts                    → Create account
 * GET    /api/accounts/{accountNumber}    → Get account details
 * GET    /api/accounts/{accountNumber}/balance → Get balance
 * PUT    /api/accounts/{accountNumber}/debit   → Debit amount
 * PUT    /api/accounts/{accountNumber}/credit  → Credit amount
 * ===========================================================
 *
 * INTERVIEW TIP:
 * Q: Who calls the debit and credit APIs?
 * A: The Payment Service calls these APIs via OpenFeign (a declarative
 *    REST client). During money transfer, Payment Service:
 *    1. Calls debit API on sender's account
 *    2. Calls credit API on receiver's account
 *    Both happen inside a @Transactional block in Payment Service.
 *
 * Q: Why not use Kafka for debit/credit?
 * A: Balance updates must be SYNCHRONOUS — we need to know IMMEDIATELY
 *    if the debit succeeded before crediting the receiver.
 *    Kafka is used AFTER the transaction commits, for downstream
 *    services (notification, fraud, audit) that don't block the payment.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // Allow Angular frontend (will be restricted via Gateway in prod)
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/accounts
     * Create a new bank account.
     *
     * Request body:
     * {
     *   "customerId": "CUST001",
     *   "customerName": "Ravi Kumar",
     *   "initialBalance": 50000.00
     * }
     *
     * Response: 201 CREATED with account details
     */
    @PostMapping
    public ResponseEntity<AccountDto.AccountResponse> createAccount(
            @Valid @RequestBody AccountDto.CreateAccountRequest request) {

        log.info("API: Create account request for customer: {}", request.getCustomerId());
        AccountDto.AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/accounts/register
     * Public alias for account creation — used by the Angular Register page.
     * This route is listed as public in api-gateway/application.yml public-paths
     * so no JWT token is required.
     *
     * Delegates to the same accountService.createAccount() — no duplicate logic.
     */
    @PostMapping("/register")
    public ResponseEntity<AccountDto.AccountResponse> register(
            @Valid @RequestBody AccountDto.CreateAccountRequest request) {

        log.info("API: Register new account for customer: {}", request.getCustomerId());
        AccountDto.AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/accounts/{accountNumber}
     * Get full account details.
     *
     * Response: 200 OK with account details, or 404 if not found
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto.AccountResponse> getAccount(
            @PathVariable String accountNumber) {

        log.info("API: Get account: {}", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    /**
     * GET /api/accounts/by-customer/{customerId}
     * Resolves a customerId (from JWT/localStorage) to full account details.
     * Called by Angular at login to fetch and store the accountNumber.
     * Allows the Dashboard to show live balance without any hardcoded mapping.
     */
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<AccountDto.AccountResponse> getAccountByCustomerId(
            @PathVariable String customerId) {

        log.info("API: Get account for customerId: {}", customerId);
        return ResponseEntity.ok(accountService.getAccountByCustomerId(customerId));
    }

    /**
     * GET /api/accounts/{accountNumber}/balance
     * Get balance only — used by Angular Dashboard.
     *
     * Response:
     * {
     *   "accountNumber": "ACC1A2B3C",
     *   "customerName": "Ravi Kumar",
     *   "balance": 40000.00,
     *   "currency": "INR"
     * }
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountDto.BalanceResponse> getBalance(
            @PathVariable String accountNumber) {

        log.info("API: Get balance for account: {}", accountNumber);
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    /**
     * PUT /api/accounts/{accountNumber}/debit
     * Debit (subtract) amount from an account.
     * Called by Payment Service via OpenFeign during transfer.
     *
     * Request:
     * { "amount": 10000.00 }
     *
     * Returns 400 if insufficient balance or account inactive.
     */
    @PutMapping("/{accountNumber}/debit")
    public ResponseEntity<AccountDto.AccountResponse> debitAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody AccountDto.BalanceUpdateRequest request) {

        log.info("API: Debit ₹{} from account: {}", request.getAmount(), accountNumber);
        return ResponseEntity.ok(accountService.debitAccount(accountNumber, request));
    }

    /**
     * PUT /api/accounts/{accountNumber}/credit
     * Credit (add) amount to an account.
     * Called by Payment Service via OpenFeign during transfer.
     *
     * Request:
     * { "amount": 10000.00 }
     */
    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<AccountDto.AccountResponse> creditAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody AccountDto.BalanceUpdateRequest request) {

        log.info("API: Credit ₹{} to account: {}", request.getAmount(), accountNumber);
        return ResponseEntity.ok(accountService.creditAccount(accountNumber, request));
    }

    /**
     * GET /api/accounts/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Account Service is running ✓");
    }
}
