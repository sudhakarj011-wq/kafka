package com.banking.audit.consumer;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit Consumer
 * Logs RAW JSON for compliance. Notice it takes ConsumerRecord<String, String>
 * rather than a parsed POJO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "payment-events", groupId = "audit-group")
    @Transactional
    public void recordAuditLog(ConsumerRecord<String, String> record) {
        log.info("Audit: logging raw event. Key: {}", record.key());

        AuditLog auditLog = AuditLog.builder()
                .topic(record.topic())
                .messageKey(record.key())
                .payload(record.value()) // Raw JSON string
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log saved successfully.");
    }
}
