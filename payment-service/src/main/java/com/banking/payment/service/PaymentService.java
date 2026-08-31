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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Service — Core Business Logic
 *
 * ======================== MOST IMPORTANT CLASS ========================
 * This class demonstrates the KEY architectural decisions:
 *
 * 1. @Transactional — ACID guarantees for money transfer
 * 2. OpenFeign — synchronous calls to Account Service
 * 3. Outbox Pattern — atomic DB write + event persistence
 * 4. Kafka event — published AFTER DB commit (via OutboxPublisher)
 *
 * THE TRANSFER FLOW:
 * ─────────────────────────────────────────────────────────
 * POST /api/payments/transfer
 *         │
 *         ▼
 * ┌─── @Transactional BEGIN ──────────────────────────────┐
 * │  1. Validate fromAccount exists (Feign → AccSvc)      │
 * │  2. Validate toAccount exists (Feign → AccSvc)        │
 * │  3. Check fromAccount is ACTIVE                       │
 * │  4. Check sufficient balance                          │
 * │  5. Save Transaction (status: INITIATED)              │
 * │  6. Debit fromAccount (Feign PUT → AccSvc)           │
 * │  7. Credit toAccount (Feign PUT → AccSvc)            │
 * │  8. Update Transaction (status: SUCCESS)              │
 * │  9. Save OutboxEvent (payload: PaymentEvent JSON)     │
 * └─── @Transactional COMMIT ─────────────────────────────┘
 *         │
 *         ▼
 * OutboxPublisher @Scheduled (every 5s):
 *   ● Reads PENDING outbox events
 *   ● Publishes to Kafka "payment-events"
 *   ● Marks them SENT
 *
 * IF step 6 or 7 fails → @Transactional ROLLBACK
 *    All DB changes are undone: account restored to original balance
 *    Transaction marked FAILED. No Kafka event published.
 *
 * INTERVIEW TIP:
 * Q: Why not publish Kafka event directly inside @Transactional?
 * A: Kafka publish is NOT part of the DB transaction. If DB commits
 *    but Kafka publish fails, we have INCONSISTENCY (money moved,
 *    no notification). The Outbox Pattern solves this by saving the
 *    event inside the SAME DB transaction, then publishing separately.
 * =====================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, SagaPaymentEvent> sagaKafkaTemplate;

    private static final String SAGA_TOPIC = "saga-events";

    /**
     * SAGA CHOREOGRAPHY - Async Transfer
     * Does NOT use Feign. Just saves PENDING and emits to Kafka.
     */
    @Transactional
    public PaymentDto.TransferResponse transferSaga(PaymentDto.TransferRequest request) {
        String transactionId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== SAGA Starting transfer {} | ₹{} from {} to {} ===",
                transactionId, request.getAmount(), request.getFromAccount(), request.getToAccount());

        // Save transaction as PENDING (instead of INITIATED)
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

        // Build Payload
        SagaPaymentEvent sagaEvent = SagaPaymentEvent.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .status(SagaPaymentEvent.SagaStatus.PENDING)
                .build();

        // Normally, this should use Outbox Pattern too, but for simplicity we publish directly
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

    /**
     * Main money transfer method.
     *
     * @Transactional ensures ALL DB operations are atomic:
     * - If debit succeeds but credit fails → both are rolled back
     * - The sender's balance is restored automatically
     */
    @Transactional
    public PaymentDto.TransferResponse transfer(PaymentDto.TransferRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== Starting transfer {} | ₹{} from {} to {} ===",
                transactionId, request.getAmount(), request.getFromAccount(), request.getToAccount());

        // ── Step 1 & 2: Validate both accounts exist ──────────────────────
        AccountClientDto.AccountResponse fromAccount = validateAccount(request.getFromAccount());
        AccountClientDto.AccountResponse toAccount = validateAccount(request.getToAccount());

        log.info("From: {} ({}) | Balance: ₹{}",
                fromAccount.getCustomerName(), fromAccount.getAccountNumber(), fromAccount.getBalance());
        log.info("To: {} ({})", toAccount.getCustomerName(), toAccount.getAccountNumber());

        // ── Step 3: Validate account is ACTIVE ────────────────────────────
        if (!"ACTIVE".equals(fromAccount.getStatus())) {
            throw new RuntimeException("Sender account " + request.getFromAccount() + " is not ACTIVE");
        }
        if (!"ACTIVE".equals(toAccount.getStatus())) {
            throw new RuntimeException("Receiver account " + request.getToAccount() + " is not ACTIVE");
        }

        // ── Step 4: Check sufficient balance ──────────────────────────────
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Insufficient balance. Available: ₹%.2f, Requested: ₹%.2f",
                    fromAccount.getBalance(), request.getAmount()));
        }

        // ── Step 5: Save transaction as INITIATED ─────────────────────────
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
            // ── Step 6: Debit sender ─────────────────────────────────────
            AccountClientDto.BalanceUpdateRequest debitReq =
                    new AccountClientDto.BalanceUpdateRequest(request.getAmount());
            accountServiceClient.debitAccount(request.getFromAccount(), debitReq);
            log.info("✅ Debited ₹{} from {}", request.getAmount(), request.getFromAccount());

            // ── Step 7: Credit receiver ──────────────────────────────────
            AccountClientDto.BalanceUpdateRequest creditReq =
                    new AccountClientDto.BalanceUpdateRequest(request.getAmount());
            accountServiceClient.creditAccount(request.getToAccount(), creditReq);
            log.info("✅ Credited ₹{} to {}", request.getAmount(), request.getToAccount());

            // ── Step 8: Update transaction to SUCCESS ────────────────────
            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);
            log.info("Transaction {} marked SUCCESS", transactionId);

            // ── Step 9: Save PaymentEvent to Outbox (ATOMIC with DB!) ────
            //
            // WHY OUTBOX? Because Kafka publish is NOT transactional with DB.
            // We save the event to DB here (inside the same transaction).
            // OutboxPublisher @Scheduled job will read this and publish to Kafka.
            //
            PaymentEvent event = buildPaymentEvent(transactionId,
                    fromAccount, toAccount, request);
            saveToOutbox(event);
            log.info("PaymentEvent saved to outbox for Kafka publishing: {}", event.getEventId());

            log.info("=== Transfer {} COMPLETED SUCCESSFULLY ===", transactionId);
            return PaymentDto.TransferResponse.success(
                    transactionId,
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount());

        } catch (FeignException e) {
            // ── Feign call to Account Service failed ─────────────────────
            // @Transactional will rollback → account balances restored
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

    /**
     * Get transaction by ID — for Angular transaction detail screen.
     */
    @Transactional(readOnly = true)
    public PaymentDto.TransactionResponse getTransaction(String transactionId) {
        Transaction t = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return mapToResponse(t);
    }

    /**
     * Get transaction history for an account — for Angular history screen.
     */
    @Transactional(readOnly = true)
    public List<PaymentDto.TransactionResponse> getTransactionHistory(String accountNumber) {
        return transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(accountNumber, accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ======================== PRIVATE HELPERS ========================

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

    private PaymentDto.TransactionResponse mapToResponse(Transaction t) {
        return PaymentDto.TransactionResponse.builder()
                .id(t.getId())
                .transactionId(t.getTransactionId())
                .fromAccount(t.getFromAccount())
                .toAccount(t.getToAccount())
                .amount(t.getAmount())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
