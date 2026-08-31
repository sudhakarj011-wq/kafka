package com.banking.notification.consumer;

import com.banking.notification.dto.PaymentEventDto;
import com.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer — Listens to "payment-events" topic.
 *
 * RESPONSIBILITY: Receive Kafka messages and delegate to NotificationService.
 * Business logic lives in NotificationService (idempotency, notification creation).
 *
 * group = "notification-group": Every message in this group goes to this consumer.
 * Within the group, Kafka distributes messages across partitions.
 *
 * INTERVIEW TIP:
 * Q: Why separate Consumer from Service?
 * A: Single Responsibility Principle. Consumer handles Kafka protocol/offset.
 *    Service handles business logic. Service is independently unit-testable
 *    without needing a running Kafka broker in tests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(PaymentEventDto event) {
        log.info("📨 Kafka Event received: eventId={}, txnId={}", event.getEventId(), event.getTransactionId());
        try {
            notificationService.processPaymentEvent(event);
        } catch (Exception e) {
            log.error("❌ Failed to process event {}: {}", event.getEventId(), e.getMessage());
            // Throwing re-triggers Kafka retry. After retries exhausted, goes to DLT.
            throw e;
        }
    }
}
