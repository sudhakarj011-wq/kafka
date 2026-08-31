package com.banking.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account Entity — maps to the 'accounts' table in MySQL
 *
 * ======================== DATABASE SCHEMA ========================
 * accounts
 * ---------
 * id             BIGINT AUTO_INCREMENT PRIMARY KEY
 * account_number VARCHAR(20) UNIQUE NOT NULL
 * customer_id    VARCHAR(20) NOT NULL
 * customer_name  VARCHAR(100) NOT NULL
 * balance        DECIMAL(15,2) NOT NULL DEFAULT 0.00
 * status         ENUM('ACTIVE','INACTIVE','SUSPENDED') DEFAULT 'ACTIVE'
 * created_at     DATETIME
 * updated_at     DATETIME
 * =================================================================
 *
 * INTERVIEW TIP:
 * Q: Why use BigDecimal for balance, not double?
 * A: In banking, floating-point precision errors are UNACCEPTABLE.
 *    BigDecimal provides exact decimal arithmetic.
 *    Example: 0.1 + 0.2 in double = 0.30000000000000004 (WRONG!)
 *             0.1 + 0.2 in BigDecimal = 0.3 (CORRECT!)
 */
@Entity
@Table(name = "accounts",
    indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    /**
     * ALWAYS use BigDecimal for monetary values in banking!
     * precision=15 supports up to 999,999,999,999,999 (15 digits)
     * scale=2 means 2 decimal places (paise)
     */
    @Column(name = "balance", nullable = false,
            precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
