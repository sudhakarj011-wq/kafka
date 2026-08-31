package com.banking.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentEvent POJO for Deserialization.
 *
 * NOTE: This is an exact copy of PaymentEvent from the Payment Service.
 *
 * INTERVIEW TIP:
 * Q: Why do you duplicate this class instead of putting it in a shared library?
 * A: In microservices, we favor LOOSE COUPLING over DRY (Don't Repeat Yourself).
 *    If Payment Service and Notification Service shared a common .jar file,
 *    updating the jar means taking down/redeploying both services.
 *    By copying the POJO, Notification Service continues to work perfectly
 *    even if Payment Service adds new fields to the event. This allows
 *    independent deployments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEventDto {

    private String eventId;
    private String eventType;
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private String fromCustomerId;
    private String toCustomerId;
    private BigDecimal amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
