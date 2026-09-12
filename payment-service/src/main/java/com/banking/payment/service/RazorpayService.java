package com.banking.payment.service;

import com.banking.payment.grpc.AccountGrpcClient;
import com.banking.payment.dto.AccountClientDto;
import com.banking.payment.dto.RazorpayDto;
import com.banking.payment.entity.OutboxEvent;
import com.banking.payment.entity.RazorpayOrder;
import com.banking.payment.entity.Transaction;
import com.banking.payment.event.PaymentEvent;
import com.banking.payment.repository.OutboxEventRepository;
import com.banking.payment.repository.RazorpayOrderRepository;
import com.banking.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Razorpay Service — handles Deposit via Razorpay Payment Gateway.
 *
 * Flow:
 * 1. createOrder()         → Creates order in Razorpay + saves RazorpayOrder (CREATED)
 * 2. verifyWebhookSignature() → HMAC-SHA256 validation (called from controller)
 * 3. handlePaymentSuccess() → Credits account, saves Transaction (DEPOSIT), writes Outbox
 *
 * IMPORTANT: Existing PaymentCommandService is NOT modified. This is fully isolated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountGrpcClient accountGrpcClient;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    // ──────────────────────────────────────────────────
    // 1. CREATE ORDER
    // ──────────────────────────────────────────────────

    /**
     * Creates a Razorpay order and persists a local record.
     * Called by: POST /api/razorpay/order
     */
    @Transactional
    public RazorpayDto.CreateOrderResponse createOrder(RazorpayDto.CreateOrderRequest request) {
        log.info("Creating Razorpay order: ₹{} for account {}", request.getAmount(), request.getAccountNumber());

        try {
            // Razorpay requires amount in paise (smallest unit)
            long amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            orderRequest.put("payment_capture", 1); // auto-capture

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");
            log.info("Razorpay Order created: {}", razorpayOrderId);

            // Persist locally
            RazorpayOrder localOrder = RazorpayOrder.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .accountNumber(request.getAccountNumber())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(RazorpayOrder.RazorpayOrderStatus.CREATED)
                    .build();
            razorpayOrderRepository.save(localOrder);

            return RazorpayDto.CreateOrderResponse.builder()
                    .orderId(razorpayOrderId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .keyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────
    // 2. VERIFY WEBHOOK SIGNATURE
    // ──────────────────────────────────────────────────

    /**
     * Razorpay signs the webhook body with HMAC-SHA256 using webhookSecret.
     * We verify to ensure the request is genuinely from Razorpay.
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            log.info("✅ Razorpay webhook signature verified");
            return true;
        } catch (RazorpayException e) {
            log.warn("❌ Invalid Razorpay webhook signature: {}", e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────
    // 3. HANDLE PAYMENT SUCCESS
    // ──────────────────────────────────────────────────

    /**
     * Called after webhook verification when event = "payment.captured".
     * Credits account, saves Transaction, writes Outbox event for Kafka.
     */
    @Transactional
    public void handlePaymentSuccess(String razorpayOrderId, String razorpayPaymentId) {
        log.info("=== Razorpay Payment Captured: order={}, payment={} ===",
                razorpayOrderId, razorpayPaymentId);

        RazorpayOrder order = razorpayOrderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Razorpay order not found: " + razorpayOrderId));

        // Idempotency guard — webhook may fire more than once
        if (order.getStatus() == RazorpayOrder.RazorpayOrderStatus.PAID) {
            log.warn("Order {} already processed. Skipping duplicate webhook.", razorpayOrderId);
            return;
        }

        String transactionId = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try {
            // Credit via account-service (gRPC)
            accountGrpcClient.creditAccount(
                    order.getAccountNumber(),
                    order.getAmount()
            );
            log.info("✅ Credited ₹{} to {}", order.getAmount(), order.getAccountNumber());

            // Save bank Transaction record
            Transaction txn = Transaction.builder()
                    .transactionId(transactionId)
                    .fromAccount("RAZORPAY_GATEWAY")
                    .toAccount(order.getAccountNumber())
                    .amount(order.getAmount())
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .description("Deposit via Razorpay | Payment ID: " + razorpayPaymentId)
                    .build();
            transactionRepository.save(txn);

            // Update Razorpay order status
            order.setStatus(RazorpayOrder.RazorpayOrderStatus.PAID);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setBankTransactionId(transactionId);
            razorpayOrderRepository.save(order);

            // Save to Outbox → OutboxPublisher picks up and publishes to Kafka
            saveDepositEventToOutbox(transactionId, order, razorpayPaymentId);
            log.info("=== Deposit {} COMPLETED. Outbox event saved for Kafka. ===", transactionId);

        } catch (Exception e) {
            order.setStatus(RazorpayOrder.RazorpayOrderStatus.FAILED);
            razorpayOrderRepository.save(order);
            log.error("Deposit failed for order {}: {}", razorpayOrderId, e.getMessage());
            throw new RuntimeException("Deposit processing failed: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────

    private void saveDepositEventToOutbox(String transactionId, RazorpayOrder order, String paymentId) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                    .eventType("DEPOSIT_SUCCESSFUL")
                    .transactionId(transactionId)
                    .fromAccount("RAZORPAY_GATEWAY")
                    .toAccount(order.getAccountNumber())
                    .fromCustomerId("RAZORPAY")
                    .toCustomerId(order.getAccountNumber())
                    .amount(order.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .aggregateType("RazorpayDeposit")
                    .aggregateId(transactionId)
                    .eventType("DEPOSIT_SUCCESSFUL")
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save deposit event to outbox: " + e.getMessage());
        }
    }
}
