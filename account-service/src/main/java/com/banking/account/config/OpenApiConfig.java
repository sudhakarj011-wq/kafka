package com.banking.account.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Account Service API",
                version = "1.0.0",
                description = "REST APIs for Account Management, Authentication, and Core Banking Operations"
        )
)
public class OpenApiConfig {
}
