package com.banking.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification Entity
 * Stores the notifications for a customer to be displayed in the Angular UI.
 */
@Entity
@Table(name = "notifications",
    indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        DEBIT_SUCCESS,
        CREDIT_SUCCESS,
        SYSTEM_ALERT
    }
}
