package com.banking.account.service;

import com.banking.account.dto.AccountDto;
import com.banking.account.entity.Account;
import com.banking.account.exception.AccountException;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * AccountService Unit Tests
 * =====================================================================
 *
 * INTERVIEW TIP — What testing tools are used here?
 *
 * @ExtendWith(MockitoExtension.class)
 *   → Activates Mockito mocking for this test class.
 *   → Replaces manual MockitoAnnotations.openMocks(this) in @BeforeEach.
 *
 * @Mock
 *   → Creates a fake/mock object that does nothing by default.
 *   → We use when(...).thenReturn(...) to program its behavior.
 *   → The real AccountRepository (which needs MySQL) is NEVER called.
 *
 * @InjectMocks
 *   → Creates a real AccountService instance and automatically injects
 *     all @Mock objects into its constructor (matches @RequiredArgsConstructor).
 *
 * AssertJ (assertThat):
 *   → Preferred over JUnit assertEquals() for fluent, readable assertions.
 *   → e.g. assertThat(result.getBalance()).isEqualByComparingTo("40000")
 *
 * PATTERN: Arrange → Act → Assert (AAA)
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    // ──────────────────────────────────────────
    // Shared Test Fixtures (reused across tests)
    // ──────────────────────────────────────────
    private Account activeAccount;
    private Account inactiveAccount;

    @BeforeEach
    void setUp() {
        // ACTIVE account — standard test account
        activeAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC1001")
                .customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("50000.00"))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // INACTIVE account — for testing account status validations
        inactiveAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC9999")
                .customerId("CUST999")
                .customerName("Suspended User")
                .balance(new BigDecimal("0.00"))
                .status(Account.AccountStatus.INACTIVE)
                .build();
    }

    // ══════════════════════════════════════════════
    // Tests: createAccount()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("createAccount: should create and return new account successfully")
    void createAccount_success() {
        // ARRANGE
        AccountDto.CreateAccountRequest request = AccountDto.CreateAccountRequest.builder()
                .customerId("CUST002")
                .customerName("Ravi Kumar")
                .initialBalance(new BigDecimal("10000.00"))
                .build();

        // Mock: account number does NOT exist (no collision)
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

        // ACT
        AccountDto.AccountResponse response = accountService.createAccount(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("CUST001"); // returned from saved mock
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    // ══════════════════════════════════════════════
    // Tests: getAccountByNumber()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("getAccountByNumber: should return account when found")
    void getAccountByNumber_found() {
        // ARRANGE
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(activeAccount));

        // ACT
        AccountDto.AccountResponse response = accountService.getAccountByNumber("ACC1001");

        // ASSERT
        assertThat(response.getAccountNumber()).isEqualTo("ACC1001");
        assertThat(response.getCustomerName()).isEqualTo("Sudhakar Reddy");
        assertThat(response.getBalance()).isEqualByComparingTo("50000.00");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getAccountByNumber: should throw AccountNotFoundException when not found")
    void getAccountByNumber_notFound() {
        // ARRANGE
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        // ACT + ASSERT
        // INTERVIEW TIP: assertThatThrownBy is the clean way to test exceptions
        assertThatThrownBy(() -> accountService.getAccountByNumber("INVALID"))
                .isInstanceOf(AccountException.AccountNotFoundException.class);
    }

    // ══════════════════════════════════════════════
    // Tests: getAccountByCustomerId()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("getAccountByCustomerId: should resolve customerId to account details")
    void getAccountByCustomerId_found() {
        // ARRANGE
        when(accountRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(activeAccount));

        // ACT
        AccountDto.AccountResponse response = accountService.getAccountByCustomerId("CUST001");

        // ASSERT
        assertThat(response.getCustomerId()).isEqualTo("CUST001");
        assertThat(response.getAccountNumber()).isEqualTo("ACC1001");
    }

    @Test
    @DisplayName("getAccountByCustomerId: should throw exception for unknown customerId")
    void getAccountByCustomerId_notFound() {
        when(accountRepository.findByCustomerId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByCustomerId("UNKNOWN"))
                .isInstanceOf(AccountException.AccountNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ══════════════════════════════════════════════
    // Tests: getBalance()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("getBalance: should return balance for valid account")
    void getBalance_success() {
        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(activeAccount));

        AccountDto.BalanceResponse balance = accountService.getBalance("ACC1001");

        assertThat(balance.getAccountNumber()).isEqualTo("ACC1001");
        assertThat(balance.getBalance()).isEqualByComparingTo("50000.00");
    }

    // ══════════════════════════════════════════════
    // Tests: debitAccount()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("debitAccount: should subtract amount and return updated account")
    void debitAccount_success() {
        // ARRANGE
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .build();

        Account afterDebit = Account.builder()
                .id(1L).accountNumber("ACC1001").customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("40000.00"))
                .status(Account.AccountStatus.ACTIVE).build();

        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(afterDebit);

        // ACT
        AccountDto.AccountResponse response = accountService.debitAccount("ACC1001", request);

        // ASSERT
        assertThat(response.getBalance()).isEqualByComparingTo("40000.00");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("debitAccount: should throw InsufficientBalanceException when balance is low")
    void debitAccount_insufficientBalance() {
        // ARRANGE: try to debit more than available
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("99999.00")) // More than 50000
                .build();

        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(activeAccount));

        // ACT + ASSERT
        assertThatThrownBy(() -> accountService.debitAccount("ACC1001", request))
                .isInstanceOf(AccountException.InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("debitAccount: should throw AccountInactiveException for INACTIVE account")
    void debitAccount_inactiveAccount() {
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findByAccountNumber("ACC9999")).thenReturn(Optional.of(inactiveAccount));

        assertThatThrownBy(() -> accountService.debitAccount("ACC9999", request))
                .isInstanceOf(AccountException.AccountInactiveException.class);
    }

    // ══════════════════════════════════════════════
    // Tests: creditAccount()
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("creditAccount: should add amount and return updated account")
    void creditAccount_success() {
        // ARRANGE
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .build();

        Account afterCredit = Account.builder()
                .id(1L).accountNumber("ACC1001").customerId("CUST001")
                .customerName("Sudhakar Reddy")
                .balance(new BigDecimal("55000.00"))
                .status(Account.AccountStatus.ACTIVE).build();

        when(accountRepository.findByAccountNumber("ACC1001")).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(afterCredit);

        // ACT
        AccountDto.AccountResponse response = accountService.creditAccount("ACC1001", request);

        // ASSERT
        assertThat(response.getBalance()).isEqualByComparingTo("55000.00");
    }

    @Test
    @DisplayName("creditAccount: should throw AccountInactiveException for INACTIVE account")
    void creditAccount_inactiveAccount() {
        AccountDto.BalanceUpdateRequest request = AccountDto.BalanceUpdateRequest.builder()
                .amount(new BigDecimal("1000.00")).build();

        when(accountRepository.findByAccountNumber("ACC9999")).thenReturn(Optional.of(inactiveAccount));

        assertThatThrownBy(() -> accountService.creditAccount("ACC9999", request))
                .isInstanceOf(AccountException.AccountInactiveException.class);
    }
}
