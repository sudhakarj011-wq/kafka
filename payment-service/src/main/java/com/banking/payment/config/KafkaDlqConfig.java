package com.banking.payment.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Dead Letter Queue (DLQ) Configuration for Payment Service.
 *
 * INTERVIEW TIP:
 * By default, if a consumer throws an exception, Kafka might loop infinitely
 * or drop the message entirely.
 *
 * This DefaultErrorHandler intercepts consumer exceptions.
 * 1. It retries processing the message 3 times with a 3-second delay between retries.
 * 2. If it still fails, it sends the original message to a Dead Letter Topic (DLT)
 *    by appending "-DLT" to the original topic name (e.g., payment-events-DLT).
 *
 * This ensures ZERO DATA LOSS in an event-driven architecture.
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
