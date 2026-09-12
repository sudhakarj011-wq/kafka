package com.banking.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Razorpay Order Entity
 * Stores Razorpay order details separately — does NOT affect the existing
 * Transaction / OutboxEvent tables.
 *
 * Lifecycle:
 *   CREATED  → when order is created via Razorpay API
 *   PAID     → when webhook confirms payment.captured
 *   FAILED   → when payment fails / webhook timeout
 */
@Entity
@Table(name = "razorpay_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Razorpay-generated order ID, e.g. order_OxyzABC123 */
    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    /** Bank account to credit when payment succeeds */
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    /** Amount in paise (₹) — e.g. ₹500 → 50000 */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RazorpayOrderStatus status = RazorpayOrderStatus.CREATED;

    /** Razorpay payment ID received in webhook after payment.captured */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /** Internal TXN-xxx ID created after wallet credit */
    @Column(name = "bank_transaction_id")
    private String bankTransactionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RazorpayOrderStatus {
        CREATED, PAID, FAILED
    }
}
