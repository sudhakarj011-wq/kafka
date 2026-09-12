package com.banking.account.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Dead Letter Queue (DLQ) Configuration for Account Service.
 *
 * INTERVIEW TIP:
 * This ensures that if the AccountService fails to process a saga command via Kafka,
 * the message is not lost but routed to a DLT (e.g., account-saga-events-DLT)
 * after 3 failed retry attempts. Engineers can then investigate and replay the DLT messages.
 */
@Configuration
public class KafkaDlqConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + "-DLT", record.partition()));
        
        // Retry 3 times, wait 3 seconds between retries
        return new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 3));
    }
}
