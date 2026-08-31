package com.banking.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * OutboxEvent Entity — maps to 'outbox_events' table in payment_db
 *
 * ======================== OUTBOX PATTERN ========================
 * Problem: After DB transaction commits, Kafka publish could fail.
 *   → Money moved, but NO notification/audit event published!
 *   → System inconsistency!
 *
 * Solution: Save event to outbox IN THE SAME @Transactional block.
 *   → If DB commits, outbox record is saved too (atomically)
 *   → A @Scheduled poller reads PENDING outbox events & publishes to Kafka
 *   → Marks them SENT after successful publish
 *
 * Flow:
 *   @Transactional {
 *     debitSender()           → DB write
 *     creditReceiver()        → DB write
 *     saveTransaction(SUCCESS) → DB write
 *     saveOutboxEvent()       → DB write  ← NEW!
 *   }  ← ALL commit or ALL rollback (atomicity!)
 *
 *   @Scheduled (every 5s):
 *     fetch PENDING outbox events
 *     publish to Kafka
 *     mark SENT
 *
 * INTERVIEW TIP:
 * Q: Why is the Outbox Pattern important in banking?
 * A: It solves the "dual-write problem". Without it, we can't atomically
 *    write to DB AND publish to Kafka. With it, we only write to DB
 *    (which supports transactions). The Kafka publish happens separately
 *    but is guaranteed to happen eventually (at-least-once delivery).
 *    This is the production-grade approach for event sourcing.
 * =================================================================
 */
@Entity
@Table(name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_status", columnList = "status"),
        @Index(name = "idx_outbox_event_id", columnList = "event_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false, length = 50)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;  // e.g., "Transaction"

    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId;    // e.g., transactionId

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;      // e.g., "PAYMENT_SUCCESSFUL"

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;        // JSON string of PaymentEvent

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public enum OutboxStatus {
        PENDING,
        SENT,
        FAILED
    }
}
