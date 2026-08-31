package com.banking.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
        System.out.println("================================================");
        System.out.println("  Audit Service started on port 8085            ");
        System.out.println("  Listening to Kafka topic (audit-group)        ");
        System.out.println("================================================");
    }
}
