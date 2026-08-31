package com.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Account Service — Main Application
 *
 * ========================== WHY THIS SERVICE? ==========================
 * In a banking microservices architecture, Account Service is responsible
 * for managing bank accounts: balances, account details, and balance updates.
 *
 * It is consumed by the Payment Service via OpenFeign (synchronous REST call)
 * because balance check and update MUST happen immediately, not asynchronously.
 *
 * INTERVIEW TIP:
 * Q: Why does Payment Service call Account Service via REST, not Kafka?
 * A: Because payment validation (check balance, debit/credit) must be
 *    SYNCHRONOUS — we need an immediate response to know if the payment
 *    succeeded before we publish the Kafka event. Kafka is used AFTER
 *    the transaction for downstream services that don't need to respond.
 * =======================================================================
 */
@SpringBootApplication
@EnableDiscoveryClient  // Register with Eureka Service Discovery
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        System.out.println("===========================================");
        System.out.println("   Account Service Started on port 8081   ");
        System.out.println("   Registered with Eureka: localhost:8761  ");
        System.out.println("===========================================");
    }
}
