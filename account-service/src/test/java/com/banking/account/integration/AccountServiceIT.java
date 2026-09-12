package com.banking.account.integration;

import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Account Service Integration Test
 * 
 * INTERVIEW TIP:
 * Why Testcontainers instead of H2 (in-memory DB)?
 * H2 has a different SQL syntax and transaction boundary behavior than MySQL.
 * If you test with H2, bugs might escape to production (e.g., MySQL locks, deadlocks).
 * Testcontainers spins up a REAL MySQL docker container during testing.
 * 
 * The @ServiceConnection (Spring Boot 3.1+) automatically injects the container's
 * URL, username, and password into the Spring context! No need for @DynamicPropertySource.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountServiceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
            .withDatabaseName("test_account_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        accountRepository.deleteAll();
    }

    @Test
    void shouldConnectToTestContainersAndSaveAccount() {
        // Assert that the real MySQL container is running
        assertThat(mysql.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();

        // 1. Arrange
        Account account = Account.builder()
                .accountNumber("ACC-TEST-100")
                .customerId("CUST-999")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status("ACTIVE")
                .build();

        // 2. Act
        Account savedAccount = accountRepository.save(account);

        // 3. Assert
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(accountRepository.findByAccountNumber("ACC-TEST-100")).isPresent();
    }
}
