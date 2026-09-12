package com.banking.account.controller;

import com.banking.account.dto.AccountDto;
import com.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =====================================================================
 * AccountController Unit Tests
 * =====================================================================
 * INTERVIEW TIP — @WebMvcTest vs @SpringBootTest?
 *
 * @SpringBootTest: Loads the ENTIRE Spring context (Beans, DB, Redis,
 *   Kafka). Slow, used for Integration Tests.
 *
 * @WebMvcTest: Loads ONLY the Web MVC layer (Controller + MockMvc).
 *   Service, Repository, DB are NOT loaded. Very FAST.
 *   We use @MockBean to replace AccountService with a mock.
 *
 * INTERVIEW TIP — What is MockMvc?
 * MockMvc lets you send fake HTTP requests to your controllers and
 * verify the HTTP status code, response headers, and response JSON body
 * WITHOUT starting a real HTTP server.
 * =====================================================================
 */
@WebMvcTest(AccountController.class)
@DisplayName("AccountController Unit Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc; // Injects MockMvc automatically by @WebMvcTest

    @MockBean
    private AccountService accountService; // Mocked — no real DB or service needed

    @Autowired
    private ObjectMapper objectMapper; // For JSON serialization

    // Shared response fixture
    private AccountDto.AccountResponse buildMockResponse() {
        return AccountDto.AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC1001")
                .customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("50000.00"))
                .status("ACTIVE")
                .build();
    }

    // ══════════════════════════════════════════════
    // Tests: POST /api/accounts
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/accounts: should create account and return 201 CREATED")
    void createAccount_returns201() throws Exception {
        // ARRANGE
        AccountDto.CreateAccountRequest request = AccountDto.CreateAccountRequest.builder()
                .customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .initialBalance(new BigDecimal("50000.00"))
                .build();

        when(accountService.createAccount(any())).thenReturn(buildMockResponse());

        // ACT + ASSERT via MockMvc
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())                      // 201
                .andExpect(jsonPath("$.accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$.customerName").value("Sudhakar Reddy"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/accounts/{accountNumber}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/accounts/ACC1001: should return 200 with account details")
    void getAccount_returns200() throws Exception {
        when(accountService.getAccountByNumber("ACC1001")).thenReturn(buildMockResponse());

        mockMvc.perform(get("/api/accounts/ACC1001"))
                .andExpect(status().isOk())                           // 200
                .andExpect(jsonPath("$.accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$.balance").value(50000.00));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/accounts/by-customer/{customerId}
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/accounts/by-customer/CUST001: should return account for customer")
    void getAccountByCustomerId_returns200() throws Exception {
        when(accountService.getAccountByCustomerId("CUST001")).thenReturn(buildMockResponse());

        mockMvc.perform(get("/api/accounts/by-customer/CUST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST001"));
    }

    // ══════════════════════════════════════════════
    // Tests: PUT /api/accounts/{accountNumber}/debit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/accounts/ACC1001/debit: should return 200 with updated balance")
    void debitAccount_returns200() throws Exception {
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .build();

        AccountDto.AccountResponse updatedResponse = AccountDto.AccountResponse.builder()
                .id(1L).accountNumber("ACC1001").customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("40000.00"))
                .status("ACTIVE").build();

        when(accountService.debitAccount(eq("ACC1001"), any())).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/accounts/ACC1001/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(40000.00));
    }

    // ══════════════════════════════════════════════
    // Tests: PUT /api/accounts/{accountNumber}/credit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/accounts/ACC1001/credit: should return 200 with updated balance")
    void creditAccount_returns200() throws Exception {
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .build();

        AccountDto.AccountResponse updatedResponse = AccountDto.AccountResponse.builder()
                .id(1L).accountNumber("ACC1001").customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("55000.00"))
                .status("ACTIVE").build();

        when(accountService.creditAccount(eq("ACC1001"), any())).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/accounts/ACC1001/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(55000.00));
    }

    // ══════════════════════════════════════════════
    // Tests: GET /api/accounts/health
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/accounts/health: should return 200 health check")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/accounts/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("running")));
    }
}
