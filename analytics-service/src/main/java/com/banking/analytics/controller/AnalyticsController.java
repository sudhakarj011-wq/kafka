package com.banking.analytics.controller;

import com.banking.analytics.entity.PaymentSummary;
import com.banking.analytics.repository.AnalyticsRepositories;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsRepositories.PaymentSummaryRepository summaryRepository;

    @GetMapping("/summary")
    public ResponseEntity<PaymentSummary> getTodaySummary() {
        return summaryRepository.findBySummaryDate(LocalDate.now())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
