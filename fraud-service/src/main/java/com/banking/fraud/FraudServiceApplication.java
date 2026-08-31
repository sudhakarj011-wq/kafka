package com.banking.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FraudServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudServiceApplication.class, args);
        System.out.println("================================================");
        System.out.println("  Fraud Detection Service started on port 8084  ");
        System.out.println("  Listening to Kafka topic (fraud-group)        ");
        System.out.println("================================================");
    }
}
