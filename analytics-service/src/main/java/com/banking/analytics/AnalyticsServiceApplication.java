package com.banking.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
        System.out.println("================================================");
        System.out.println("  Analytics Service started on port 8086        ");
        System.out.println("  Listening to Kafka topic (analytics-group)    ");
        System.out.println("================================================");
    }
}
