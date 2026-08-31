package com.banking.analytics.consumer;

import com.banking.analytics.dto.PaymentEventDto;
import com.banking.analytics.entity.PaymentSummary;
import com.banking.analytics.entity.ProcessedEvent;
import com.banking.analytics.repository.AnalyticsRepositories;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final AnalyticsRepositories.PaymentSummaryRepository summaryRepository;
    private final AnalyticsRepositories.ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "payment-events", groupId = "analytics-group")
    @Transactional
    public void aggregateData(PaymentEventDto event) {
        log.info("Analytics: Processing event {}", event.getEventId());

        // Idempotency Check
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("⚠️ Duplicate event skipped in analytics: {}", event.getEventId());
            return;
        }

        if ("PAYMENT_SUCCESSFUL".equals(event.getEventType())) {
            LocalDate today = LocalDate.now();

            // Fetch today's summary or create new
            PaymentSummary summary = summaryRepository.findBySummaryDate(today)
                .orElse(PaymentSummary.builder()
                        .summaryDate(today)
                        .totalTransactions(0L)
                        .totalVolume(BigDecimal.ZERO)
                        .build());

            // Aggregate
            summary.setTotalTransactions(summary.getTotalTransactions() + 1);
            summary.setTotalVolume(summary.getTotalVolume().add(event.getAmount()));
            summaryRepository.save(summary);
            
            log.info("Analytics Updated: Date={} Txns={} Vol=₹{}", 
                    today, summary.getTotalTransactions(), summary.getTotalVolume());
        }

        // Mark processed
        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(event.getEventId());
        processedEventRepository.save(processed);
    }
}
