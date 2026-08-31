package com.banking.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentEvent — The Kafka Message (Event)
 *
 * ======================== KAFKA EVENT DESIGN ========================
 * This is the object that Payment Service publishes to Kafka.
 * All 4 consumer services (Notification, Fraud, Audit, Analytics)
 * receive and deserialize this exact object.
 *
 * IMPORTANT FIELDS:
 *
 * eventId → Used for IDEMPOTENCY
 *   Each event has a globally unique ID (UUID).
 *   Consumers check: "Have I already processed eventId=EVT-abc123?"
 *   If yes → skip. This prevents duplicate processing!
 *
 * eventType → Allows future event types
 *   Currently: PAYMENT_SUCCESSFUL, PAYMENT_FAILED
 *   Future: PAYMENT_REFUNDED, PAYMENT_REVERSED, etc.
 *   Consumers can filter by eventType.
 *
 * transactionId → Used as Kafka MESSAGE KEY
 *   hash(transactionId) % numPartitions = partition number
 *   Ensures all events for same transaction go to same partition.
 *   Within a partition, ORDER is guaranteed!
 *
 * SAMPLE KAFKA MESSAGE:
 * {
 *   "eventId": "EVT-f47ac10b-58cc",
 *   "eventType": "PAYMENT_SUCCESSFUL",
 *   "transactionId": "TXN-3c5f04a4",
 *   "fromAccount": "ACC1A2B3C",
 *   "toAccount": "ACCDEF456",
 *   "amount": 10000.00,
 *   "fromCustomerId": "CUST001",
 *   "toCustomerId": "CUST002",
 *   "timestamp": "2026-08-27T20:30:00"
 * }
 * ===================================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    /**
     * Unique event ID — used for idempotency check in consumers.
     * Generated: "EVT-" + UUID
     */
    private String eventId;

    /**
     * Type of event — allows consumers to filter relevant events.
     * Values: PAYMENT_SUCCESSFUL, PAYMENT_FAILED
     */
    private String eventType;

    /**
     * Transaction ID — used as Kafka message KEY for partition ordering.
     * Also links the event back to the transactions table in payment_db.
     */
    private String transactionId;

    /** Sender's account number */
    private String fromAccount;

    /** Receiver's account number */
    private String toAccount;

    /** Sender's customer ID — used by Notification Service to find customer */
    private String fromCustomerId;

    /** Receiver's customer ID */
    private String toCustomerId;

    /** Transfer amount */
    private BigDecimal amount;

    /** When the payment event was created (after DB commit) */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
