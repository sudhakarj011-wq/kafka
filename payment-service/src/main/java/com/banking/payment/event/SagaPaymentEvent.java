package com.banking.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Saga Choreography Event Payload
 * Sent back and forth between Payment Service and Account Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaPaymentEvent {
    
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    
    private SagaStatus status;
    private String message;

    public enum SagaStatus {
        PENDING,  // Sent by Payment Service to initiate
        SUCCESS,  // Sent by Account Service if debit/credit works
        FAILED    // Sent by Account Service if debit/credit fails (Compensating action trigger)
    }
}
