package com.banking.fraud.consumer;

import com.banking.fraud.dto.PaymentEventDto;
import com.banking.fraud.entity.FraudTransaction;
import com.banking.fraud.repository.FraudTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Fraud Detection Consumer
 * Listens to "payment-events" independently via "fraud-group".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudConsumer {

    private final FraudTransactionRepository fraudRepository;

    @KafkaListener(topics = "payment-events", groupId = "fraud-group")
    @Transactional
    public void analyzeTransaction(PaymentEventDto event) {
        log.info("Fraud Check: Analyzing transaction {}", event.getTransactionId());

        // Simple Idempotency: Check if we already analyzed this transaction
        if (fraudRepository.existsByTransactionId(event.getTransactionId())) {
            log.warn("⚠️ Duplicate transaction analysis skipped: {}", event.getTransactionId());
            return;
        }

        // 1. SIMPLE FRAUD RULES ENGINE
        FraudTransaction.RiskLevel riskLevel = FraudTransaction.RiskLevel.LOW_RISK;
        String reason = "Normal transaction";

        // Rule: Amount greater than ₹1,00,000 is HIGH RISK
        if (event.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            riskLevel = FraudTransaction.RiskLevel.HIGH_RISK;
            reason = "Amount flagged as unusually high";
        }

        // In a real system, you'd check redis cache for velocity:
        // "Has this account sent >5 transactions in the last hour?"

        // 2. SAVE FRAUD RECORD
        FraudTransaction fraudTx = FraudTransaction.builder()
                .transactionId(event.getTransactionId())
                .fromAccount(event.getFromAccount())
                .toAccount(event.getToAccount())
                .amount(event.getAmount())
                .riskLevel(riskLevel)
                .reason(reason)
                .build();
        
        fraudRepository.save(fraudTx);
        log.info("Fraud Check Complete for {}: [{}] - {}", 
                event.getTransactionId(), riskLevel, reason);
    }
}
