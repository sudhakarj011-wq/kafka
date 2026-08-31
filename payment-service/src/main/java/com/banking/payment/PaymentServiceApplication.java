package com.banking.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service — Main Application
 *
 * ======================== PAYMENT SERVICE ROLE ========================
 * This is the CORE service of the banking system. It is responsible for:
 *
 * 1. Receiving transfer requests from Angular via API Gateway
 * 2. Validating accounts (via OpenFeign → Account Service)
 * 3. Executing the transfer in a SINGLE @Transactional block
 * 4. Publishing PaymentEvent to Kafka AFTER DB commits
 *
 * KEY ANNOTATIONS:
 *
 * @EnableFeignClients → Activates OpenFeign for REST client proxies.
 *   OpenFeign lets us write interfaces instead of RestTemplate boilerplate.
 *   Example: accountClient.debitAccount("ACC1001", request) → REST call!
 *
 * @EnableScheduling → Needed for Phase 10 (Outbox Pattern) where a
 *   scheduled job polls the outbox table every 5 seconds.
 *
 * INTERVIEW TIP:
 * Q: Why is Payment Service the Kafka PRODUCER and not the others?
 * A: Payment Service owns the "payment happened" event. It's the source
 *    of truth — it has the DB transaction. After committing to DB, it
 *    publishes ONE event. All other services (notification, fraud, audit)
 *    are CONSUMERS — they react to what Payment Service did.
 * ======================================================================
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        System.out.println("===========================================");
        System.out.println("   Payment Service Started on port 8082   ");
        System.out.println("   Kafka Producer: localhost:9092          ");
        System.out.println("   Topic: payment-events (4 partitions)   ");
        System.out.println("===========================================");
    }
}
