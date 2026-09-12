package com.banking.payment.service;

import com.banking.payment.dto.PaymentDto;
import com.banking.payment.entity.Transaction;
import com.banking.payment.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * PaymentQueryService Unit Tests
 * =====================================================================
 * INTERVIEW TIP — Why test Query services separately from Command?
 * CQRS Pattern: Command (write) and Query (read) are separated.
 * PaymentQueryService only READS data — no side effects, no Kafka,
 * no gRPC. Testing it is clean and isolated: just mock Repository.
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryService Unit Tests")
class PaymentQueryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    // Shared test fixture
    private Transaction buildTransaction(String txnId, String from, String to) {
        return Transaction.builder()
                .id(1L)
                .transactionId(txnId)
                .fromAccount(from)
                .toAccount(to)
                .amount(new BigDecimal("5000.00"))
                .status(Transaction.TransactionStatus.SUCCESS)
                .description("Test transfer")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════════
    // Tests: getTransaction()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("getTransaction: should return transaction details for valid ID")
    void getTransaction_found() {
        // ARRANGE
        Transaction txn = buildTransaction("TXN-ABC123", "ACC1001", "ACC1002");
        when(transactionRepository.findByTransactionId("TXN-ABC123")).thenReturn(Optional.of(txn));

        // ACT
        PaymentDto.TransactionResponse response = paymentQueryService.getTransaction("TXN-ABC123");

        // ASSERT
        assertThat(response.getTransactionId()).isEqualTo("TXN-ABC123");
        assertThat(response.getFromAccount()).isEqualTo("ACC1001");
        assertThat(response.getToAccount()).isEqualTo("ACC1002");
        assertThat(response.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getTransaction: should throw RuntimeException for unknown transaction ID")
    void getTransaction_notFound() {
        // ARRANGE
        when(transactionRepository.findByTransactionId("INVALID")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> paymentQueryService.getTransaction("INVALID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ══════════════════════════════════════════════
    // Tests: getTransactionHistory()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("getTransactionHistory: should return list of transactions for account")
    void getTransactionHistory_success() {
        // ARRANGE: two transactions involving ACC1001
        List<Transaction> mockList = List.of(
                buildTransaction("TXN-001", "ACC1001", "ACC1002"),
                buildTransaction("TXN-002", "ACC1003", "ACC1001")
        );
        when(transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc("ACC1001", "ACC1001"))
                .thenReturn(mockList);

        // ACT
        List<PaymentDto.TransactionResponse> history = paymentQueryService.getTransactionHistory("ACC1001");

        // ASSERT
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTransactionId()).isEqualTo("TXN-001");
        assertThat(history.get(1).getTransactionId()).isEqualTo("TXN-002");
    }

    @Test
    @DisplayName("getTransactionHistory: should return empty list when no transactions exist")
    void getTransactionHistory_empty() {
        when(transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc("ACC9999", "ACC9999"))
                .thenReturn(List.of());

        List<PaymentDto.TransactionResponse> history = paymentQueryService.getTransactionHistory("ACC9999");

        assertThat(history).isEmpty();
    }
}
