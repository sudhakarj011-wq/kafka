package com.banking.analytics.repository;

import com.banking.analytics.entity.PaymentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, Long> {
    Optional<PaymentSummary> findBySummaryDate(LocalDate date);
}
