package com.banking.notification.service;

import com.banking.notification.dto.PaymentEventDto;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.ProcessedEvent;
import com.banking.notification.repository.NotificationRepository;
import com.banking.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationService — Business Logic Layer
 *
 * Extracted from NotificationConsumer to follow proper
 * layered architecture: Consumer → Service → Repository
 *
 * INTERVIEW TIP:
 * Q: Why extract business logic from the consumer?
 * A: Single Responsibility Principle. The consumer's job is
 *    to receive Kafka messages and delegate. The service's
 *    job is to apply business rules (idempotency check,
 *    notification creation). This makes the service
 *    independently testable without a Kafka broker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Process a payment event received from Kafka.
     * Implements idempotency: if the eventId was already processed, it is skipped.
     *
     * @Transactional ensures both the Notification record and the
     * ProcessedEvent record are saved atomically.
     * If DB fails mid-way, both are rolled back.
     * Kafka offset is NOT committed if an exception is thrown.
     */
    @Transactional
    public void processPaymentEvent(PaymentEventDto event) {
        log.info("Processing event: eventId={}, txnId={}", event.getEventId(), event.getTransactionId());

        // IDEMPOTENCY CHECK — Kafka delivers at-least-once; avoid processing duplicates
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("⚠️ Duplicate event skipped: {}", event.getEventId());
            return;
        }

        if ("PAYMENT_SUCCESSFUL".equals(event.getEventType())) {

            // Notify Sender (Debit notification)
            Notification senderNotification = Notification.builder()
                    .customerId(event.getFromCustomerId())
                    .type(Notification.NotificationType.DEBIT_SUCCESS)
                    .message(String.format("Your account %s was debited by ₹%.2f for txn %s",
                            event.getFromAccount(), event.getAmount(), event.getTransactionId()))
                    .build();
            notificationRepository.save(senderNotification);

            // Notify Receiver (Credit notification)
            Notification receiverNotification = Notification.builder()
                    .customerId(event.getToCustomerId())
                    .type(Notification.NotificationType.CREDIT_SUCCESS)
                    .message(String.format("Your account %s was credited with ₹%.2f from txn %s",
                            event.getToAccount(), event.getAmount(), event.getTransactionId()))
                    .build();
            notificationRepository.save(receiverNotification);

            log.info("✅ Notifications saved for sender ({}) and receiver ({})",
                    event.getFromCustomerId(), event.getToCustomerId());
        }

        // Mark this event as processed (idempotency record)
        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(event.getEventId());
        processedEventRepository.save(processed);
    }

    /**
     * Fetch all notifications for a customer, most recent first.
     * Used by NotificationController — REST API for Angular frontend.
     */
    public List<Notification> getNotificationsForCustomer(String customerId) {
        log.info("Fetching notifications for customer: {}", customerId);
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
