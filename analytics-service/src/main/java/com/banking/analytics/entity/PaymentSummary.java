package com.banking.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PaymentSummary Entity
 * Aggregates daily transaction data.
 */
@Entity
@Table(name = "payment_summary",
    indexes = {
        @Index(name = "idx_summary_date", columnList = "summary_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(name = "total_transactions", nullable = false)
    private Long totalTransactions;

    @Column(name = "total_volume", nullable = false)
    private BigDecimal totalVolume;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}
