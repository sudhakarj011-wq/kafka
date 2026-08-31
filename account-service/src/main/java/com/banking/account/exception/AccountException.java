package com.banking.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exceptions for clear error semantics.
 * Using @ResponseStatus to auto-map to HTTP status codes.
 */
public class AccountException {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String accountNumber, java.math.BigDecimal available) {
            super(String.format("Insufficient balance in account %s. Available: ₹%.2f",
                    accountNumber, available));
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String accountNumber) {
            super("Account " + accountNumber + " is not ACTIVE");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateAccountException extends RuntimeException {
        public DuplicateAccountException(String accountNumber) {
            super("Account number already exists: " + accountNumber);
        }
    }
}
