package com.banking.payment.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTOs for communicating with Account Service via OpenFeign.
 * These mirror the response DTOs from Account Service exactly.
 * They are used ONLY internally by Payment Service client calls.
 */
public class AccountClientDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountResponse {
        private Long id;
        private String accountNumber;
        private String customerId;
        private String customerName;
        private BigDecimal balance;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceUpdateRequest {
        private BigDecimal amount;
    }
}
