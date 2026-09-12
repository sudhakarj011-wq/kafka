package com.banking.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Payment Service API",
                version = "1.0.0",
                description = "REST APIs for Payment Processing, Saga Transfers, and Razorpay Integration"
        )
)
public class OpenApiConfig {
}
