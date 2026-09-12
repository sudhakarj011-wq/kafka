package com.banking.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Razorpay-specific DTOs — isolated from existing PaymentDto.
 */
public class RazorpayDto {

    /**
     * Frontend sends this to create a Razorpay order.
     * POST /api/razorpay/order
     * {
     *   "amount": 500.00,
     *   "accountNumber": "ACC1001",
     *   "currency": "INR"
     * }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateOrderRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.0", message = "Minimum deposit is ₹1")
        @DecimalMax(value = "500000.00", message = "Maximum single deposit is ₹5,00,000")
        private BigDecimal amount;

        @NotBlank(message = "Account number is required")
        private String accountNumber;

        @Builder.Default
        private String currency = "INR";
    }

    /**
     * Backend returns this to frontend so it can open Razorpay checkout popup.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateOrderResponse {
        private String orderId;       // Razorpay order_xxx ID
        private BigDecimal amount;    // in rupees (frontend converts to paise for SDK)
        private String currency;
        private String keyId;         // Razorpay publishable key (safe to expose)
    }

    /**
     * Razorpay webhook payload after payment.captured event.
     * Razorpay sends the full JSON body — we only map what we need.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookPayload {
        private String event;          // "payment.captured"
        private WebhookPayloadData payload;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WebhookPayloadData {
            private PaymentEntity payment;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class PaymentEntity {
                private PaymentItem entity;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class PaymentItem {
                private String id;             // razorpay payment_id
                private String order_id;       // razorpay order_id
                private String status;         // "captured"
                private Long amount;           // in paise
                private String currency;
            }
        }
    }
}
