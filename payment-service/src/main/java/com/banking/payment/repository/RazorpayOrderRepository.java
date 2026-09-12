package com.banking.payment.repository;

import com.banking.payment.entity.RazorpayOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Razorpay Order Repository
 * Separate from TransactionRepository — no cross-table impact.
 */
@Repository
public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {

    Optional<RazorpayOrder> findByRazorpayOrderId(String razorpayOrderId);
}
