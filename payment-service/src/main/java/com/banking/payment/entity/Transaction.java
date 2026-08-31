package com.banking.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Entity — maps to 'transactions' table in payment_db
 *
 * ======================== DATABASE SCHEMA ========================
 * transactions
 * ------------
 * id              BIGINT AUTO_INCREMENT PRIMARY KEY
 * transaction_id  VARCHAR(36) UNIQUE NOT NULL   ← UUID
 * from_account    VARCHAR(20) NOT NULL
 * to_account      VARCHAR(20) NOT NULL
 * amount          DECIMAL(15,2) NOT NULL
 * status          ENUM('INITIATED','SUCCESS','FAILED')
 * description     VARCHAR(255)
 * created_at      DATETIME
 * updated_at      DATETIME
 * =================================================================
 *
 * TRANSACTION STATUS LIFECYCLE:
 *
 *   Payment Request arrives
 *         │
 *         ▼
 *    INITIATED ← transaction saved before debit/credit
 *         │
 *         ├── debit & credit succeed → SUCCESS → Kafka event published
 *         │
 *         └── any error → FAILED → no Kafka event
 *
 * INTERVIEW TIP:
 * Q: Why save transaction as INITIATED first, before debit/credit?
 * A: If the service crashes AFTER debit but BEFORE saving the transaction,
 *    we'd have no record. By saving INITIATED first (inside @Transactional),
 *    either the entire block (debit + credit + save) commits or all rollback.
 *    This ensures we ALWAYS have a transaction record.
 */
@Entity
@Table(name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_from_account", columnList = "from_account"),
        @Index(name = "idx_to_account", columnList = "to_account")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "from_account", nullable = false, length = 20)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 20)
    private String toAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionStatus {
        INITIATED,
        SUCCESS,
        FAILED
    }
}
