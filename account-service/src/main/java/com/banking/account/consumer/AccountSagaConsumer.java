package com.banking.account.consumer;

import com.banking.account.dto.AccountDto;
import com.banking.account.event.SagaPaymentEvent;
import com.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga Participant (Choreography)
 * Listens to "saga-events". Process debits/credits via AccountService.
 * Replies with SUCCESS or FAILED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountSagaConsumer {

    private final AccountService accountService;
    private final KafkaTemplate<String, SagaPaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String SAGA_TOPIC = "saga-events";

    @KafkaListener(topics = SAGA_TOPIC, groupId = "account-service-saga-group")
    @Transactional
    public void consumeSagaEvent(String message) {
        try {
            SagaPaymentEvent event = objectMapper.readValue(message, SagaPaymentEvent.class);

            // We only process PENDING events. (Ignore SUCCESS/FAILED which are handled by Payment Service)
            if (event.getStatus() != SagaPaymentEvent.SagaStatus.PENDING) {
                return;
            }

            log.info("Saga [Account-Service] Received PENDING transfer: TxnId={}, Amount={}", 
                    event.getTransactionId(), event.getAmount());

            try {
                // Execute business logic (both debit and credit in same TX due to @Transactional)
                AccountDto.UpdateBalanceRequest debitReq = new AccountDto.UpdateBalanceRequest(event.getAmount());
                accountService.debit(event.getFromAccount(), debitReq);

                AccountDto.UpdateBalanceRequest creditReq = new AccountDto.UpdateBalanceRequest(event.getAmount());
                accountService.credit(event.getToAccount(), creditReq);

                // If successful, reply with SUCCESS
                event.setStatus(SagaPaymentEvent.SagaStatus.SUCCESS);
                event.setMessage("Account debited and credited successfully.");
                log.info("Saga [Account-Service] Transfer successful! Emitting SUCCESS.");

            } catch (Exception businessError) {
                // If insufficient balance, account missing, etc., it comes here.
                // Rollback happens automatically for the DB, but we must explicitly reply with FAILED.
                log.warn("Saga [Account-Service] Transfer failed! Emitting FAILED. Reason: {}", businessError.getMessage());
                event.setStatus(SagaPaymentEvent.SagaStatus.FAILED);
                event.setMessage(businessError.getMessage());
            }

            // Publish the result back to the same Kafka Topic
            kafkaTemplate.send(SAGA_TOPIC, event.getTransactionId(), event);

        } catch (Exception e) {
            log.error("Saga [Account-Service] Kafka deserialization error: {}", e.getMessage());
        }
    }
}
