package com.banking.fraud.controller;

import com.banking.fraud.entity.FraudTransaction;
import com.banking.fraud.repository.FraudTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Fraud Controller — REST API Layer
 *
 * GET /api/fraud/transactions              → All fraud-analyzed transactions
 * GET /api/fraud/transactions/{txnId}      → Single fraud record by transactionId
 * GET /api/fraud/transactions/risk/{level} → Filter by risk level (LOW_RISK / HIGH_RISK)
 * GET /api/fraud/health                    → Health check
 */
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudController {

    private final FraudTransactionRepository fraudRepository;

    /**
     * GET /api/fraud/transactions
     * Returns all fraud records — latest first (sorted by DB default).
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<FraudTransaction>> getAllFraudTransactions() {
        log.info("API: Fetching all fraud transactions");
        List<FraudTransaction> records = fraudRepository.findAll();
        return ResponseEntity.ok(records);
    }

    /**
     * GET /api/fraud/transactions/{transactionId}
     * Returns fraud analysis result for a specific payment transaction.
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<FraudTransaction> getFraudByTransactionId(
            @PathVariable String transactionId) {

        log.info("API: Fetching fraud record for transactionId: {}", transactionId);
        return fraudRepository.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/fraud/transactions/risk/{level}
     * Returns all transactions of a given risk level: LOW_RISK or HIGH_RISK.
     */
    @GetMapping("/transactions/risk/{level}")
    public ResponseEntity<List<FraudTransaction>> getByRiskLevel(
            @PathVariable String level) {

        log.info("API: Fetching fraud transactions with risk level: {}", level);
        try {
            FraudTransaction.RiskLevel riskLevel = FraudTransaction.RiskLevel.valueOf(level.toUpperCase());
            List<FraudTransaction> records = fraudRepository.findByRiskLevel(riskLevel);
            return ResponseEntity.ok(records);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid risk level requested: {}", level);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/fraud/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Service is running ✓ | Kafka Consumer: fraud-group → payment-events");
    }
}
