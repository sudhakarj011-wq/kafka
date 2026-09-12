package com.banking.payment.controller;

import com.banking.payment.dto.RazorpayDto;
import com.banking.payment.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Razorpay Controller — Payment Gateway REST API
 *
 * Endpoints:
 *   POST /api/razorpay/order    → Create Razorpay order (JWT protected via gateway)
 *   POST /api/razorpay/webhook  → Razorpay webhook (NO JWT — uses HMAC signature)
 *
 * ISOLATION: This controller is completely independent of PaymentController.
 *            Zero changes to any existing controller or service.
 */
@RestController
@RequestMapping("/api/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayController {

    private final RazorpayService razorpayService;

    // ─────────────────────────────────────────────────────
    // 1. CREATE ORDER
    // Called by Angular frontend before opening Razorpay popup
    // ─────────────────────────────────────────────────────
    @PostMapping("/order")
    public ResponseEntity<RazorpayDto.CreateOrderResponse> createOrder(
            @Valid @RequestBody RazorpayDto.CreateOrderRequest request) {

        log.info("API: Create Razorpay order ₹{} for account {}",
                request.getAmount(), request.getAccountNumber());
        RazorpayDto.CreateOrderResponse response = razorpayService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // 2. WEBHOOK — Razorpay calls this after payment is captured
    //
    // Security: JWT is NOT used here (Razorpay can't send JWT).
    //           Instead, we verify the HMAC-SHA256 signature using
    //           the webhook secret (set in Razorpay Dashboard).
    //
    // Header: X-Razorpay-Signature: <sha256_hmac>
    // ─────────────────────────────────────────────────────
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("API: Razorpay webhook received. Event payload length: {}", rawPayload.length());

        // Step 1: Verify signature
        boolean isValid = razorpayService.verifyWebhookSignature(rawPayload, signature);
        if (!isValid) {
            log.warn("Razorpay webhook signature mismatch — rejected");
            return ResponseEntity.status(400).body(Map.of("status", "invalid_signature"));
        }

        // Step 2: Parse the event type
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            RazorpayDto.WebhookPayload webhookPayload = mapper.readValue(rawPayload, RazorpayDto.WebhookPayload.class);

            String event = webhookPayload.getEvent();
            log.info("Razorpay webhook event: {}", event);

            // Step 3: Handle payment.captured
            if ("payment.captured".equals(event)) {
                RazorpayDto.WebhookPayload.WebhookPayloadData.PaymentItem payment =
                        webhookPayload.getPayload().getPayment().getEntity();

                String razorpayOrderId = payment.getOrder_id();
                String razorpayPaymentId = payment.getId();

                log.info("Processing payment.captured: orderId={}, paymentId={}",
                        razorpayOrderId, razorpayPaymentId);

                razorpayService.handlePaymentSuccess(razorpayOrderId, razorpayPaymentId);
                return ResponseEntity.ok(Map.of("status", "ok"));
            }

            // Acknowledge other events without processing
            log.info("Razorpay event '{}' acknowledged but not handled", event);
            return ResponseEntity.ok(Map.of("status", "acknowledged"));

        } catch (Exception e) {
            log.error("Razorpay webhook processing error: {}", e.getMessage());
            // Return 200 to avoid Razorpay retrying for non-payment errors
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Razorpay Integration Active ✓ | Webhook: /api/razorpay/webhook");
    }
}
