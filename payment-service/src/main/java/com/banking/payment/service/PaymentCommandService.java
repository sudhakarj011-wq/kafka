package com.banking.payment.service;

import com.banking.payment.client.AccountServiceClient;
import com.banking.payment.dto.AccountClientDto;
import com.banking.payment.dto.PaymentDto;
import com.banking.payment.entity.OutboxEvent;
import com.banking.payment.entity.Transaction;
import com.banking.payment.event.PaymentEvent;
import com.banking.payment.event.SagaPaymentEvent;
import com.banking.payment.repository.OutboxEventRepository;
import com.banking.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Command Service
 * Handles data modification operations involving transactions and money transfers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, SagaPaymentEvent> sagaKafkaTemplate;

    private static final String SAGA_TOPIC = "saga-events";

    @Transactional
    public PaymentDto.TransferResponse transferSaga(PaymentDto.TransferRequest request) {
        String transactionId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== SAGA Starting transfer {} | ₹{} from {} to {} ===",
                transactionId, request.getAmount(), request.getFromAccount(), request.getToAccount());

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription() + " [SAGA]")
                .build();
        transactionRepository.save(transaction);
        log.info("Saga Transaction saved: {} [PENDING]", transactionId);

        SagaPaymentEvent sagaEvent = SagaPaymentEvent.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status(SagaPaymentEvent.SagaStatus.PENDING)
                .build();

        sagaKafkaTemplate.send(SAGA_TOPIC, transactionId, sagaEvent);
        log.info("Saga Event Published to Kafka: {}", transactionId);

        return PaymentDto.TransferResponse.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status("PENDING")
                .message("Transfer accepted and is processing asynchronously via Saga.")
                .build();
    }

    @Transactional
    public PaymentDto.TransferResponse transfer(PaymentDto.TransferRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== Starting transfer {} | ₹{} from {} to {} ===",
                transactionId, request.getAmount(), request.getFromAccount(), request.getToAccount());

        AccountClientDto.AccountResponse fromAccount = validateAccount(request.getFromAccount());
        AccountClientDto.AccountResponse toAccount = validateAccount(request.getToAccount());

        log.info("From: {} ({}) | Balance: ₹{}",
                fromAccount.getCustomerName(), fromAccount.getAccountNumber(), fromAccount.getBalance());
        log.info("To: {} ({})", toAccount.getCustomerName(), toAccount.getAccountNumber());

        if (!"ACTIVE".equals(fromAccount.getStatus())) {
            throw new RuntimeException("Sender account " + request.getFromAccount() + " is not ACTIVE");
        }
        if (!"ACTIVE".equals(toAccount.getStatus())) {
            throw new RuntimeException("Receiver account " + request.getToAccount() + " is not ACTIVE");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Insufficient balance. Available: ₹%.2f, Requested: ₹%.2f",
                    fromAccount.getBalance(), request.getAmount()));
        }

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.INITIATED)
                .description(request.getDescription())
                .build();
        transactionRepository.save(transaction);
        log.info("Transaction saved: {} [INITIATED]", transactionId);

        try {
            AccountClientDto.BalanceUpdateRequest debitReq =
                    new AccountClientDto.BalanceUpdateRequest(request.getAmount());
            accountServiceClient.debitAccount(request.getFromAccount(), debitReq);
            log.info("✅ Debited ₹{} from {}", request.getAmount(), request.getFromAccount());

            AccountClientDto.BalanceUpdateRequest creditReq =
                    new AccountClientDto.BalanceUpdateRequest(request.getAmount());
            accountServiceClient.creditAccount(request.getToAccount(), creditReq);
            log.info("✅ Credited ₹{} to {}", request.getAmount(), request.getToAccount());

            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);
            log.info("Transaction {} marked SUCCESS", transactionId);

            PaymentEvent event = buildPaymentEvent(transactionId, fromAccount, toAccount, request);
            saveToOutbox(event);
            log.info("PaymentEvent saved to outbox for Kafka publishing: {}", event.getEventId());

            log.info("=== Transfer {} COMPLETED SUCCESSFULLY ===", transactionId);
            return PaymentDto.TransferResponse.success(
                    transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount());

        } catch (FeignException e) {
            log.error("Account Service call failed for {}: {}", transactionId, e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Transfer failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Transfer failed {}: {}", transactionId, e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    private AccountClientDto.AccountResponse validateAccount(String accountNumber) {
        try {
            return accountServiceClient.getAccount(accountNumber);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Account not found: " + accountNumber);
        } catch (FeignException e) {
            throw new RuntimeException("Account Service unavailable: " + e.getMessage());
        }
    }

    private PaymentEvent buildPaymentEvent(String transactionId,
                                            AccountClientDto.AccountResponse from,
                                            AccountClientDto.AccountResponse to,
                                            PaymentDto.TransferRequest request) {
        return PaymentEvent.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .eventType("PAYMENT_SUCCESSFUL")
                .transactionId(transactionId)
                .fromAccount(from.getAccountNumber())
                .toAccount(to.getAccountNumber())
                .fromCustomerId(from.getCustomerId())
                .toCustomerId(to.getCustomerId())
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void saveToOutbox(PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .aggregateType("Transaction")
                    .aggregateId(event.getTransactionId())
                    .eventType(event.getEventType())
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save event to outbox: " + e.getMessage());
        }
    }
}
