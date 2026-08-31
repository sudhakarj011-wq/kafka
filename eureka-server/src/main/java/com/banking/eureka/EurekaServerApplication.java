package com.banking.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server
 *
 * ======================== EUREKA EXPLAINED ========================
 * Problem in microservices:
 *   Account Service runs on host-A:8081
 *   Payment Service needs to call Account Service
 *   But Payment Service doesn't know the IP of Account Service!
 *   (Especially in containers — IPs change on restart!)
 *
 * Solution: Eureka Service Registry
 *
 * HOW IT WORKS:
 * ┌─────────────────────────────────────────────────────┐
 * │                  EUREKA SERVER                       │
 * │  Registry:                                           │
 * │  ┌─────────────────────┬────────────────────────┐   │
 * │  │  SERVICE NAME       │  HOST:PORT             │   │
 * │  ├─────────────────────┼────────────────────────┤   │
 * │  │  account-service    │  192.168.1.100:8081    │   │
 * │  │  payment-service    │  192.168.1.101:8082    │   │
 * │  │  notification-svc   │  192.168.1.102:8083    │   │
 * │  └─────────────────────┴────────────────────────┘   │
 * └─────────────────────────────────────────────────────┘
 *
 * FLOW:
 * 1. Account Service starts → registers with Eureka
 * 2. Payment Service's Feign calls "account-service"
 * 3. Feign asks Eureka: "Where is account-service?"
 * 4. Eureka returns: "192.168.1.100:8081"
 * 5. Feign calls http://192.168.1.100:8081/api/accounts/...
 *
 * UI: http://localhost:8761 shows all registered services!
 *
 * INTERVIEW TIP:
 * Q: Netflix Eureka vs Consul vs Kubernetes DNS?
 * A: Eureka — Java/Spring friendly, self-contained
 *    Consul — Language agnostic, also does config management
 *    Kubernetes DNS — In K8s, each service gets a DNS name
 *    automatically. No separate registry needed. Use Eureka
 *    only when NOT on Kubernetes.
 * =================================================================
 */
@SpringBootApplication
@EnableEurekaServer  // This single annotation turns the app into a registry!
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("================================================");
        System.out.println("  Eureka Service Registry started on port 8761  ");
        System.out.println("  Dashboard: http://localhost:8761               ");
        System.out.println("================================================");
    }
}
