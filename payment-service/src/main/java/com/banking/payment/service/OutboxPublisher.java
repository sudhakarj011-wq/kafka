package com.banking.payment.service;

import com.banking.payment.entity.OutboxEvent;
import com.banking.payment.event.PaymentEvent;
import com.banking.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Publisher — Reads PENDING outbox events and publishes to Kafka
 *
 * ======================== OUTBOX PATTERN PUBLISHER ========================
 *
 * This service runs as a background job every 5 seconds.
 * It reads PENDING events from the outbox table and publishes them to Kafka.
 *
 * WHY IS THIS NEEDED?
 * ─────────────────────────────────────────────────────────────────────────
 * Problem (Naive approach):
 *   @Transactional
 *   void transfer() {
 *     debitSender();        ← DB write
 *     creditReceiver();     ← DB write
 *     DB commits ✅
 *     kafkaTemplate.send()  ← Kafka publish ❌ (separate system!)
 *   }
 *
 *   If Kafka is down or throws exception AFTER DB commits:
 *   → Money is moved ✅ but NO event published ❌
 *   → Notification service never knows ❌
 *   → INCONSISTENCY! ❌
 *
 * Solution (Outbox Pattern):
 *   @Transactional
 *   void transfer() {
 *     debitSender();           ← DB write
 *     creditReceiver();        ← DB write
 *     saveOutboxEvent();       ← DB write (SAME transaction!)
 *     DB commits atomically ✅ (all or nothing)
 *   }
 *
 *   @Scheduled (every 5s) → OutboxPublisher:
 *     fetch PENDING outbox events
 *     publish to Kafka
 *     mark SENT
 *
 *   GUARANTEE: If DB committed, the event WILL eventually be published.
 *
 * INTERVIEW TIP:
 * Q: What if the publisher crashes after publishing but before marking SENT?
 * A: The event will be published again (at-least-once delivery).
 *    That's why consumers implement IDEMPOTENCY — they check the eventId
 *    and skip already-processed events.
 *
 * Q: Why not use Kafka transactions (exactly-once)?
 * A: Kafka transactions work with a Kafka transactional producer, but
 *    they can't span a DB + Kafka transaction. The Outbox Pattern is the
 *    industry-standard solution for this cross-system atomicity problem.
 * =========================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    /**
     * Runs every 5 seconds to publish pending outbox events to Kafka.
     *
     * fixedDelay=5000 means: wait 5s AFTER the previous execution finishes.
     * This prevents overlapping executions if publishing takes > 5s.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;  // Nothing to publish
        }

        log.info("OutboxPublisher: found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                // Deserialize JSON payload back to PaymentEvent
                PaymentEvent paymentEvent = objectMapper.readValue(
                        outboxEvent.getPayload(), PaymentEvent.class);

                // Publish to Kafka
                // KEY = transactionId → consistent partition assignment (ordering!)
                kafkaTemplate.send(
                        PAYMENT_EVENTS_TOPIC,
                        paymentEvent.getTransactionId(),  // KEY
                        paymentEvent                       // VALUE
                );

                // Mark outbox event as SENT
                outboxEvent.setStatus(OutboxEvent.OutboxStatus.SENT);
                outboxEvent.setSentAt(LocalDateTime.now());
                outboxEventRepository.save(outboxEvent);

                log.info("✅ Published to Kafka [{}] eventId={} txnId={}",
                        PAYMENT_EVENTS_TOPIC,
                        paymentEvent.getEventId(),
                        paymentEvent.getTransactionId());

            } catch (Exception e) {
                // Mark as FAILED — won't retry automatically
                // In production, add retry logic or alert here
                outboxEvent.setStatus(OutboxEvent.OutboxStatus.FAILED);
                outboxEventRepository.save(outboxEvent);
                log.error("❌ Failed to publish outbox event {}: {}",
                        outboxEvent.getEventId(), e.getMessage());
            }
        }
    }
}
