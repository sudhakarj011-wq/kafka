package com.banking.payment.integration;

import com.banking.payment.entity.Payment;
import com.banking.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment Service Integration Test
 * 
 * INTERVIEW TIP:
 * A true integration test validates all the major components of a system.
 * By using @Testcontainers for MySQL, we test the actual JPA/Hibernate dialact 
 * behaviour instead of a fake in-memory database like H2.
 * We could also write a Kafka producer test using @EmbeddedKafka or a real KafkaContainer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentServiceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
            .withDatabaseName("test_payment_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldConnectToMySQLContainerAndSavePayment() {
        // Assert that the real MySQL container is running
        assertThat(mysql.isRunning()).isTrue();

        // 1. Arrange
        Payment payment = Payment.builder()
                .fromAccount("ACC-1")
                .toAccount("ACC-2")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status("PENDING")
                .build();

        // 2. Act
        Payment savedPayment = paymentRepository.save(payment);

        // 3. Assert
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(paymentRepository.findById(savedPayment.getId())).isPresent();
    }
}
