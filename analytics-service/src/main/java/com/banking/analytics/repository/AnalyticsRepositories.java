package com.banking.analytics.repository;

import com.banking.analytics.entity.PaymentSummary;
import com.banking.analytics.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

public class AnalyticsRepositories {

    @Repository
    public interface PaymentSummaryRepository extends JpaRepository<PaymentSummary, Long> {
        Optional<PaymentSummary> findBySummaryDate(LocalDate date);
    }

    @Repository
    public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
        boolean existsByEventId(String eventId);
    }
}
