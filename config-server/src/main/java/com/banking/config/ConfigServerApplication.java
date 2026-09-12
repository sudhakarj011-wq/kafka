package com.banking.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server Application
 * 
 * INTERVIEW TIP:
 * A Config Server centralizes all environment-specific configurations
 * (dev, qa, prod) in one place. Instead of each microservice having hardcoded
 * properties in its application.yml, they fetch config from this server on startup.
 * 
 * By default, this uses a Git-backed repository, but for our local environment,
 * we will use the 'native' profile to serve configs from local files.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
