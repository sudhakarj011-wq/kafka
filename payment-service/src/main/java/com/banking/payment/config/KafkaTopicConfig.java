package com.banking.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Configuration
 *
 * ======================== TOPIC DESIGN ========================
 * Topic: payment-events
 * Partitions: 4
 * Replicas: 1 (dev — use 3 in production!)
 *
 * WHY 4 PARTITIONS?
 * We have 4 consumer groups (notification, fraud, audit, analytics).
 * Each group can run up to 4 parallel consumers (one per partition).
 * More partitions = more parallelism = higher throughput.
 *
 * PARTITION ASSIGNMENT (Round-robin or key-based):
 * We use transactionId as the KEY → consistent hashing
 * hash("TXN-001") % 4 = 2 → always goes to Partition 2
 * This guarantees: all events for TXN-001 are in the SAME partition,
 * which guarantees ORDER for that transaction.
 *
 * PRODUCTION GUIDE:
 * - replicas(3): 3 brokers, so 2 can fail without data loss
 * - min.insync.replicas=2: at least 2 replicas must ack
 * - acks=all in producer: wait for all ISR replicas to confirm
 *
 * INTERVIEW TIP:
 * Q: How does partition count affect consumer parallelism?
 * A: A consumer group can have AT MOST as many active consumers
 *    as partitions. If you have 4 partitions and start 5 consumers,
 *    the 5th consumer sits idle (no partition to read from).
 *    So partitions = maximum parallelism ceiling.
 * =============================================================
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Creates the "payment-events" topic in Kafka.
     * Spring Kafka calls this at startup — if topic exists, it's a no-op.
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(4)
                .replicas(1)
                .build();
    }

    /**
     * Dead Letter Topic (DLT) — for failed events after all retries.
     *
     * If Notification Service fails to process an event 3 times,
     * Kafka moves it here for manual inspection/replay.
     *
     * INTERVIEW TIP:
     * Q: What is a DLT and why is it important?
     * A: DLT = Dead Letter Topic. In production, some messages may fail
     *    processing (bad data, service bug, timeout). After N retries,
     *    we can't keep retrying forever — it would block other messages.
     *    DLT stores these "poisoned messages" for investigation.
     *    Operations team can replay them after fixing the bug.
     */
    @Bean
    public NewTopic paymentEventsDlt() {
        return TopicBuilder.name("payment-events.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
