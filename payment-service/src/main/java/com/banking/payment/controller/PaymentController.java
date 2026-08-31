package com.banking.payment.controller;

import com.banking.payment.dto.PaymentDto;
import com.banking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment Controller — REST API Layer
 *
 * ===================== REST APIs =====================
 * POST /api/payments/transfer          → Transfer money
 * GET  /api/payments/{transactionId}   → Get transaction
 * GET  /api/payments/history/{account} → Transaction history
 * GET  /api/payments/health            → Health check
 * =====================================================
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/transfer
     *
     * The main banking API — transfers money between accounts.
     *
     * Request:
     * {
     *   "fromAccount": "ACC1A2B3C",
     *   "toAccount":   "ACCDEF456",
     *   "amount":      10000.00,
     *   "description": "Monthly rent"
     * }
     *
     * Success Response (200 OK):
     * {
     *   "transactionId": "TXN-3C5F04A4",
     *   "status": "SUCCESS",
     *   "message": "Transfer of ₹10000.00 successful! 🎉",
     *   "fromAccount": "ACC1A2B3C",
     *   "toAccount": "ACCDEF456",
     *   "amount": 10000.00,
     *   "timestamp": "2026-08-27T20:30:00"
     * }
     *
     * After this API returns:
     * → Database has committed debit + credit + transaction record
     * → Outbox event saved (to be published to Kafka within 5 seconds)
     * → Angular shows "Payment Successful!" screen
     */
    @PostMapping("/transfer")
    public ResponseEntity<PaymentDto.TransferResponse> transfer(
            @Valid @RequestBody PaymentDto.TransferRequest request) {

        log.info("API: Transfer request ₹{} from {} to {}",
                request.getAmount(), request.getFromAccount(), request.getToAccount());
        PaymentDto.TransferResponse response = paymentService.transfer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/transfer/saga
     *
     * NEW: Asynchronous Saga Choreography Example
     * Does not block waiting for the Account Service. Returns 202 Accepted.
     */
    @PostMapping("/transfer/saga")
    public ResponseEntity<PaymentDto.TransferResponse> transferSaga(
            @Valid @RequestBody PaymentDto.TransferRequest request) {

        log.info("API [SAGA]: Async Transfer request ₹{} from {} to {}",
                request.getAmount(), request.getFromAccount(), request.getToAccount());
        PaymentDto.TransferResponse response = paymentService.transferSaga(request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * GET /api/payments/{transactionId}
     *
     * Get transaction details by ID.
     * Used by Angular to show transaction status after transfer.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentDto.TransactionResponse> getTransaction(
            @PathVariable String transactionId) {

        log.info("API: Get transaction: {}", transactionId);
        return ResponseEntity.ok(paymentService.getTransaction(transactionId));
    }

    /**
     * GET /api/payments/history/{accountNumber}
     *
     * Get all transactions for an account (as sender or receiver).
     * Used by Angular Transaction History screen.
     */
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<PaymentDto.TransactionResponse>> getHistory(
            @PathVariable String accountNumber) {

        log.info("API: Get transaction history for: {}", accountNumber);
        return ResponseEntity.ok(paymentService.getTransactionHistory(accountNumber));
    }

    /**
     * GET /api/payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running ✓ | Kafka Producer: payment-events");
    }
}
