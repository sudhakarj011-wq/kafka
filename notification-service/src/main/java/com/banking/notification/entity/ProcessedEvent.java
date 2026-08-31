package com.banking.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ProcessedEvent Entity — IDEMPOTENCY KEY
 *
 * ======================== IDEMPOTENCY CONCEPT ========================
 * What is Idempotency?
 * An operation is idempotent if running it multiple times has the same
 * effect as running it once.
 *
 * Why is it needed in Kafka Consumers?
 * Kafka guarantees "At-Least-Once" delivery. This means Kafka might
 * deliver the SAME event TWICE (e.g., if a consumer crashes before
 * committing its offset, or due to network retries).
 *
 * The Problem:
 * If we receive `eventId="EVT-123"` twice, we don't want to send
 * the customer TWO "Payment Successful" emails!
 *
 * The Solution:
 * 1. Read event (eventId="EVT-123")
 * 2. Check `processed_events` table: Does EVT-123 exist?
 * 3. If YES → Skip processing! (We already did it).
 * 4. If NO → Process event AND save "EVT-123" to the table (atomically).
 *
 * INTERVIEW TIP:
 * Q: How do you handle duplicate messages in Kafka?
 * A: I implement Idempotency using an "Inbox/Processed Events" table.
 *    By checking the unique `eventId` before processing, we guarantee
 *    Exactly-Once semantics at the business level.
 * =====================================================================
 */
@Entity
@Table(name = "processed_events",
    indexes = {
        @Index(name = "idx_event_id", columnList = "event_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false, length = 50)
    private String eventId;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;
}
