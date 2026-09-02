package com.banking.payment.consumer;

import com.banking.payment.entity.Transaction;
import com.banking.payment.event.SagaPaymentEvent;
import com.banking.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Saga Orchestrator / Completer
 * Listens for SUCCESS or FAILED events from Account Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    private static final String SAGA_TOPIC = "saga-events";

    @KafkaListener(topics = SAGA_TOPIC, groupId = "payment-service-saga-group")
    @Transactional
    public void consumeSagaReply(String message) {
        try {
            SagaPaymentEvent event = objectMapper.readValue(message, SagaPaymentEvent.class);

            // Payment Service only cares about replies (SUCCESS or FAILED)
            if (event.getStatus() == SagaPaymentEvent.SagaStatus.PENDING) {
                return;
            }

            log.info("Saga [Payment-Service] Received reply: TxnId={}, Status={}", 
                    event.getTransactionId(), event.getStatus());

            Transaction transaction = transactionRepository.findByTransactionId(event.getTransactionId())
                    .orElseThrow(() -> new RuntimeException("Saga Transaction not found"));

            if (event.getStatus() == SagaPaymentEvent.SagaStatus.SUCCESS) {
                transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
                log.info("Saga [Payment-Service] Transaction {} COMPLETED successfully.", event.getTransactionId());
            } else if (event.getStatus() == SagaPaymentEvent.SagaStatus.FAILED) {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                // In a real app we'd save event.getMessage() to a failure_reason column
                log.info("Saga [Payment-Service] COMPENSATING ACTION. Transaction {} FAILED. Reason: {}", 
                        event.getTransactionId(), event.getMessage());
            }

            transactionRepository.save(transaction);

        } catch (Exception e) {
            log.error("Saga [Payment-Service] Failed to process reply: {}", e.getMessage());
        }
    }
}
