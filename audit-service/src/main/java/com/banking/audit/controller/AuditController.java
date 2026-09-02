package com.banking.audit.controller;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Audit Controller — REST API Layer
 *
 * GET /api/audit/logs                     → All audit logs (compliance view)
 * GET /api/audit/logs/key/{messageKey}    → Logs by Kafka message key (transactionId)
 * GET /api/audit/health                   → Health check
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * GET /api/audit/logs
     * Returns all audit log entries — for compliance/admin view.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        log.info("API: Fetching all audit logs");
        List<AuditLog> logs = auditLogRepository.findAll();
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/audit/logs/key/{messageKey}
     * Returns audit logs for a specific Kafka message key (usually the transactionId).
     * Useful for correlating a payment event to its full raw JSON audit trail.
     */
    @GetMapping("/logs/key/{messageKey}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByKey(
            @PathVariable String messageKey) {

        log.info("API: Fetching audit logs for messageKey: {}", messageKey);
        List<AuditLog> logs = auditLogRepository.findByMessageKey(messageKey);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/audit/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Audit Service is running ✓ | Kafka Consumer: audit-group → payment-events");
    }
}
