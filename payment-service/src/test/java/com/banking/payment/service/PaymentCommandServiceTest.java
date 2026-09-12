package com.banking.payment.service;

import com.banking.payment.dto.AccountClientDto;
import com.banking.payment.dto.PaymentDto;
import com.banking.payment.entity.Transaction;
import com.banking.payment.event.SagaPaymentEvent;
import com.banking.payment.grpc.AccountGrpcClient;
import com.banking.payment.repository.OutboxEventRepository;
import com.banking.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * PaymentCommandService Unit Tests
 * =====================================================================
 * INTERVIEW TIP — How do you test Kafka calls without a real broker?
 * We mock KafkaTemplate using @Mock. We then verify that
 * kafkaTemplate.send(...) was called with the correct topic and event.
 * The actual message is never sent — it's just a recording.
 *
 * INTERVIEW TIP — How do you test gRPC client calls?
 * AccountGrpcClient is mocked. When debitAccount() is called in the
 * service, Mockito records the call. We verify the method was actually
 * invoked with the correct parameters using verify(...).
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandService Unit Tests")
class PaymentCommandServiceTest {

    @Mock private AccountGrpcClient accountGrpcClient;
    @Mock private TransactionRepository transactionRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private KafkaTemplate<String, SagaPaymentEvent> sagaKafkaTemplate;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    // Shared fixtures
    private PaymentDto.TransferRequest transferRequest;
    private AccountClientDto.AccountResponse fromAccountResponse;
    private AccountClientDto.AccountResponse toAccountResponse;

    @BeforeEach
    void setUp() {
        transferRequest = PaymentDto.TransferRequest.builder()
                .fromAccount("ACC1001")
                .toAccount("ACC1002")
                .amount(new BigDecimal("5000.00"))
                .description("Test transfer")
                .build();

        fromAccountResponse = AccountClientDto.AccountResponse.builder()
                .accountNumber("ACC1001")
                .customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("50000.00"))
                .status("ACTIVE")
                .build();

        toAccountResponse = AccountClientDto.AccountResponse.builder()
                .accountNumber("ACC1002")
                .customerId("CUST002")
                .customerName("Ravi Kumar")
                .balance(new BigDecimal("20000.00"))
                .status("ACTIVE")
                .build();
    }

    // ══════════════════════════════════════════════
    // Tests: transfer() — Synchronous transfer
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("transfer: should complete transfer and return SUCCESS response")
    void transfer_success() throws Exception {
        // ARRANGE
        when(accountGrpcClient.getAccount("ACC1001")).thenReturn(fromAccountResponse);
        when(accountGrpcClient.getAccount("ACC1002")).thenReturn(toAccountResponse);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // Return whatever is saved
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // ACT
        PaymentDto.TransferResponse response = paymentCommandService.transfer(transferRequest);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getFromAccount()).isEqualTo("ACC1001");
        assertThat(response.getToAccount()).isEqualTo("ACC1002");
        assertThat(response.getAmount()).isEqualByComparingTo("5000.00");

        // VERIFY gRPC calls were made with correct arguments
        verify(accountGrpcClient).debitAccount("ACC1001", new BigDecimal("5000.00"));
        verify(accountGrpcClient).creditAccount("ACC1002", new BigDecimal("5000.00"));

        // VERIFY transaction was saved twice (INITIATED + SUCCESS)
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("transfer: should throw exception when sender has insufficient balance")
    void transfer_insufficientBalance() {
        // ARRANGE: from account has only 1000, but trying to send 5000
        AccountClientDto.AccountResponse poorAccount = AccountClientDto.AccountResponse.builder()
                .accountNumber("ACC1001").customerId("CUST001").customerName("Poor User")
                .balance(new BigDecimal("1000.00")).status("ACTIVE").build();

        when(accountGrpcClient.getAccount("ACC1001")).thenReturn(poorAccount);
        when(accountGrpcClient.getAccount("ACC1002")).thenReturn(toAccountResponse);

        // ACT + ASSERT
        assertThatThrownBy(() -> paymentCommandService.transfer(transferRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient balance");

        // VERIFY: debit should NEVER be called since validation failed
        verify(accountGrpcClient, never()).debitAccount(anyString(), any());
    }

    @Test
    @DisplayName("transfer: should throw exception when sender account is INACTIVE")
    void transfer_senderInactive() {
        // ARRANGE
        AccountClientDto.AccountResponse inactiveAccount = AccountClientDto.AccountResponse.builder()
                .accountNumber("ACC1001").customerId("CUST001").customerName("Blocked User")
                .balance(new BigDecimal("50000.00")).status("INACTIVE").build();

        when(accountGrpcClient.getAccount("ACC1001")).thenReturn(inactiveAccount);
        when(accountGrpcClient.getAccount("ACC1002")).thenReturn(toAccountResponse);

        // ACT + ASSERT
        assertThatThrownBy(() -> paymentCommandService.transfer(transferRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    @DisplayName("transfer: should handle gRPC failure and mark transaction as FAILED")
    void transfer_grpcFailure() throws Exception {
        // ARRANGE: gRPC debit throws exception
        when(accountGrpcClient.getAccount("ACC1001")).thenReturn(fromAccountResponse);
        when(accountGrpcClient.getAccount("ACC1002")).thenReturn(toAccountResponse);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("gRPC service unavailable"))
                .when(accountGrpcClient).debitAccount(anyString(), any());

        // ACT + ASSERT
        assertThatThrownBy(() -> paymentCommandService.transfer(transferRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transfer failed");

        // VERIFY: transaction was saved as INITIATED then FAILED
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        // VERIFY: credit should never be called if debit failed
        verify(accountGrpcClient, never()).creditAccount(anyString(), any());
    }

    // ══════════════════════════════════════════════
    // Tests: transferSaga() — Asynchronous Saga
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("transferSaga: should publish Kafka event and return PENDING response")
    void transferSaga_success() {
        // ARRANGE
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(sagaKafkaTemplate.send(anyString(), anyString(), any(SagaPaymentEvent.class)))
                .thenReturn(null);

        // ACT
        PaymentDto.TransferResponse response = paymentCommandService.transferSaga(transferRequest);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getMessage()).containsIgnoringCase("asynchronously");

        // VERIFY Kafka event was published to saga-events topic
        verify(sagaKafkaTemplate).send(eq("saga-events"), anyString(), any(SagaPaymentEvent.class));
        // VERIFY transaction saved with PENDING status
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }
}
