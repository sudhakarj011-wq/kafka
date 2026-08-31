package com.banking.account.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTOs (Data Transfer Objects) for Account Service REST APIs.
 *
 * INTERVIEW TIP:
 * Q: Why use DTO instead of returning Entity directly?
 * A: 1. SECURITY: Avoid exposing internal database fields (e.g., id, timestamps)
 *    2. FLEXIBILITY: API shape can differ from DB schema
 *    3. VALIDATION: DTOs carry request validation annotations
 *    4. VERSIONING: API can change without touching entity
 */
public class AccountDto {

    /**
     * Request DTO for creating a new bank account.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateAccountRequest {

        @NotBlank(message = "Customer ID is required")
        @Size(min = 3, max = 20, message = "Customer ID must be between 3-20 characters")
        private String customerId;

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Customer name must be between 2-100 characters")
        private String customerName;

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
        @Digits(integer = 13, fraction = 2, message = "Invalid balance format")
        private BigDecimal initialBalance;
    }

    /**
     * Request DTO for debit/credit operations.
     * Used by Payment Service via OpenFeign to transfer money.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceUpdateRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
        private BigDecimal amount;
    }

    /**
     * Response DTO for account details.
     * Sent back to client — never expose raw entity.
     */
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

    /**
     * Response DTO for balance queries.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceResponse {
        private String accountNumber;
        private String customerName;
        private BigDecimal balance;
        private String currency;

        // Convenience factory
        public static BalanceResponse of(String accountNumber,
                                         String customerName,
                                         BigDecimal balance) {
            return BalanceResponse.builder()
                    .accountNumber(accountNumber)
                    .customerName(customerName)
                    .balance(balance)
                    .currency("INR")
                    .build();
        }
    }
}
