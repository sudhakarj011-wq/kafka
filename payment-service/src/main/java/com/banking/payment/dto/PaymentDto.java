package com.banking.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Service DTOs
 */
public class PaymentDto {

    /**
     * Request DTO — from Angular via API Gateway.
     *
     * Sample request:
     * POST /api/payments/transfer
     * {
     *   "fromAccount": "ACC1A2B3C",
     *   "toAccount":   "ACCDEF456",
     *   "amount":      10000.00,
     *   "description": "Monthly rent payment"
     * }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferRequest {

        @NotBlank(message = "Sender account number is required")
        private String fromAccount;

        @NotBlank(message = "Receiver account number is required")
        private String toAccount;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.0", message = "Minimum transfer amount is ₹1")
        @DecimalMax(value = "10000000.00", message = "Maximum single transfer is ₹1,00,00,000")
        @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
        private BigDecimal amount;

        @Size(max = 255, message = "Description too long")
        private String description;
    }

    /**
     * Response DTO after a transfer attempt.
     *
     * SUCCESS response:
     * {
     *   "transactionId": "TXN-3c5f04a4",
     *   "status": "SUCCESS",
     *   "message": "Transfer of ₹10,000 successful!",
     *   "fromAccount": "ACC1A2B3C",
     *   "toAccount": "ACCDEF456",
     *   "amount": 10000.00,
     *   "timestamp": "2026-08-27T20:30:00"
     * }
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferResponse {
        private String transactionId;
        private String status;
        private String message;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private LocalDateTime timestamp;

        public static TransferResponse success(String txnId, String from,
                                               String to, BigDecimal amount) {
            return TransferResponse.builder()
                    .transactionId(txnId)
                    .status("SUCCESS")
                    .message(String.format("Transfer of ₹%.2f successful! 🎉", amount))
                    .fromAccount(from)
                    .toAccount(to)
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        public static TransferResponse failed(String txnId, String reason,
                                              String from, String to, BigDecimal amount) {
            return TransferResponse.builder()
                    .transactionId(txnId)
                    .status("FAILED")
                    .message("Transfer failed: " + reason)
                    .fromAccount(from)
                    .toAccount(to)
                    .amount(amount)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Response DTO for transaction history item.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionResponse {
        private Long id;
        private String transactionId;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String status;
        private String description;
        private LocalDateTime createdAt;
    }
}
