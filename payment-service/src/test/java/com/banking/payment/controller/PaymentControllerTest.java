package com.banking.payment.controller;

import com.banking.payment.dto.PaymentDto;
import com.banking.payment.service.PaymentCommandService;
import com.banking.payment.service.PaymentQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =====================================================================
 * PaymentController Unit Tests
 * =====================================================================
 * INTERVIEW TIP — Why use jsonPath()?
 * jsonPath() uses JSONPath expressions to navigate and assert specific
 * fields in the JSON response. E.g.:
 *   $.status        → root level "status" field
 *   $.fromAccount   → root level "fromAccount" field
 *   $[0].status     → first element's "status" in a JSON array
 * =====================================================================
 */
@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentCommandService paymentCommandService;

    @MockBean
    private PaymentQueryService paymentQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    // Shared fixture builder
    private PaymentDto.TransferResponse buildTransferResponse(String status) {
        return PaymentDto.TransferResponse.builder()
                .transactionId("TXN-ABC123")
                .fromAccount("ACC1001")
                .toAccount("ACC1002")
                .amount(new BigDecimal("5000.00"))
                .status(status)
                .message("Transfer " + status)
                .build();
    }

    private PaymentDto.TransferRequest buildTransferRequest() {
        return PaymentDto.TransferRequest.builder()
                .fromAccount("ACC1001")
                .toAccount("ACC1002")
                .amount(new BigDecimal("5000.00"))
                .description("Test payment")
                .build();
    }

    // ══════════════════════════════════════════════
    // Tests: POST /api/payments/transfer
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/payments/transfer: should return 200 with SUCCESS status")
    void transfer_returns200() throws Exception {
        when(paymentCommandService.transfer(any())).thenReturn(buildTransferResponse("SUCCESS"));

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isOk())                           // 200
                .andExpect(jsonPath("$.transactionId").value("TXN-ABC123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fromAccount").value("ACC1001"));
    }

    // ══════════════════════════════════════════════
    // Tests: POST /api/payments/transfer/saga
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/payments/transfer/saga: should return 202 ACCEPTED with PENDING status")
    void transferSaga_returns202() throws Exception {
        when(paymentCommandService.transferSaga(any())).thenReturn(buildTransferResponse("PENDING"));

        mockMvc.perform(post("/api/payments/transfer/saga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTransferRequest())))
                .andExpect(status().isAccepted())                     // 202
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/payments/{transactionId}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/payments/TXN-ABC123: should return transaction details")
    void getTransaction_returns200() throws Exception {
        PaymentDto.TransactionResponse txnResponse = PaymentDto.TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN-ABC123")
                .fromAccount("ACC1001")
                .toAccount("ACC1002")
                .amount(new BigDecimal("5000.00"))
                .status("SUCCESS")
                .description("Test transfer")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentQueryService.getTransaction("TXN-ABC123")).thenReturn(txnResponse);

        mockMvc.perform(get("/api/payments/TXN-ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-ABC123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/payments/history/{accountNumber}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/payments/history/ACC1001: should return list of transactions")
    void getHistory_returns200() throws Exception {
        List<PaymentDto.TransactionResponse> history = List.of(
                PaymentDto.TransactionResponse.builder()
                        .transactionId("TXN-001").fromAccount("ACC1001").toAccount("ACC1002")
                        .amount(new BigDecimal("1000.00")).status("SUCCESS")
                        .createdAt(LocalDateTime.now()).build(),
                PaymentDto.TransactionResponse.builder()
                        .transactionId("TXN-002").fromAccount("ACC1003").toAccount("ACC1001")
                        .amount(new BigDecimal("2000.00")).status("FAILED")
                        .createdAt(LocalDateTime.now()).build()
        );

        when(paymentQueryService.getTransactionHistory("ACC1001")).thenReturn(history);

        mockMvc.perform(get("/api/payments/history/ACC1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))           // Array size = 2
                .andExpect(jsonPath("$[0].transactionId").value("TXN-001"))
                .andExpect(jsonPath("$[1].status").value("FAILED"));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/payments/health
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/payments/health: should return 200")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Service")));
    }
}
