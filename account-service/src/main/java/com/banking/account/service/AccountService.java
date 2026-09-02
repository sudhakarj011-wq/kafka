package com.banking.account.service;

import com.banking.account.dto.AccountDto;
import com.banking.account.entity.Account;
import com.banking.account.exception.AccountException;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Account Service — Business Logic Layer
 *
 * ======================== SERVICE LAYER ROLE ========================
 * The Service layer contains all BUSINESS RULES.
 * Controllers only receive HTTP requests and delegate to Service.
 * Repository only talks to the database.
 *
 * This is the SINGLE RESPONSIBILITY PRINCIPLE in action:
 * - Controller: HTTP concerns
 * - Service: Business logic
 * - Repository: Data access
 * ====================================================================
 *
 * INTERVIEW TIP:
 * Q: Why put @Transactional on service methods, not controller?
 * A: The transaction should wrap business logic, not HTTP handling.
 *    Service methods may call multiple repository methods that
 *    must all succeed or all fail together (atomicity).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Create a new bank account.
     * Generates a unique account number automatically.
     *
     * @Transactional — if anything fails, the INSERT is rolled back.
     */
    @Transactional
    public AccountDto.AccountResponse createAccount(AccountDto.CreateAccountRequest request) {
        log.info("Creating account for customer: {}", request.getCustomerId());

        // Generate unique account number: ACC + 6 random digits
        String accountNumber = generateAccountNumber();

        // Check uniqueness (very unlikely collision but safeguard)
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateAccountNumber();
        }

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .balance(request.getInitialBalance())
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created: {} for customer: {}", accountNumber, request.getCustomerId());

        return mapToResponse(saved);
    }

    /**
     * Get account details by account number.
     * Served from Redis if present, else queries MySQL.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountDto.AccountResponse getAccountByNumber(String accountNumber) {
        log.info("Fetching account: {}", accountNumber);
        Account account = findAccountOrThrow(accountNumber);
        return mapToResponse(account);
    }

    /**
     * Get account details by customer ID.
     * Used by Angular frontend after login — resolves customerId → accountNumber.
     */
    @Transactional(readOnly = true)
    public AccountDto.AccountResponse getAccountByCustomerId(String customerId) {
        log.info("Fetching account for customerId: {}", customerId);
        Account account = accountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new AccountException.AccountNotFoundException("Customer: " + customerId));
        return mapToResponse(account);
    }

    /**
     * Get account balance.
     * Used by Angular dashboard and Payment Service validation.
     */
    @Transactional(readOnly = true)
    public AccountDto.BalanceResponse getBalance(String accountNumber) {
        log.info("Fetching balance for: {}", accountNumber);
        Account account = findAccountOrThrow(accountNumber);
        return AccountDto.BalanceResponse.of(
                account.getAccountNumber(),
                account.getCustomerName(),
                account.getBalance()
        );
    }

    /**
     * Debit (subtract) amount from account.
     * Called by Payment Service via OpenFeign during money transfer.
     *
     * IMPORTANT: This method validates:
     * 1. Account exists
     * 2. Account is ACTIVE
     * 3. Sufficient balance available
     *
     * @Transactional — ensures balance update is atomic.
     * @CachePut — updates the Redis cache directly with the new AccountResponse.
     */
    @Transactional
    @CachePut(value = "accounts", key = "#accountNumber")
    public AccountDto.AccountResponse debitAccount(String accountNumber,
                                                    AccountDto.BalanceUpdateRequest request) {
        log.info("Debiting account: {} amount: ₹{}", accountNumber, request.getAmount());

        Account account = findAccountOrThrow(accountNumber);

        // Validate account is active
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountException.AccountInactiveException(accountNumber);
        }

        // Validate sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new AccountException.InsufficientBalanceException(
                    accountNumber, account.getBalance());
        }

        // Debit: subtract the amount
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);

        log.info("Debited ₹{} from account {}. New balance: ₹{}",
                request.getAmount(), accountNumber, newBalance);

        return mapToResponse(updated);
    }

    /**
     * Credit (add) amount to account.
     * Called by Payment Service via OpenFeign during money transfer.
     *
     * @Transactional — ensures atomic update.
     * @CachePut — ensures the updated account info overrides the stale info in Redis.
     */
    @Transactional
    @CachePut(value = "accounts", key = "#accountNumber")
    public AccountDto.AccountResponse creditAccount(String accountNumber,
                                                     AccountDto.BalanceUpdateRequest request) {
        log.info("Crediting account: {} amount: ₹{}", accountNumber, request.getAmount());

        Account account = findAccountOrThrow(accountNumber);

        // Validate account is active
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountException.AccountInactiveException(accountNumber);
        }

        // Credit: add the amount
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);

        log.info("Credited ₹{} to account {}. New balance: ₹{}",
                request.getAmount(), accountNumber, newBalance);

        return mapToResponse(updated);
    }

    // ======================== HELPER METHODS ========================

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException.AccountNotFoundException(accountNumber));
    }

    private String generateAccountNumber() {
        // Format: ACC + 6 random uppercase hex chars → e.g. ACC-A3F8B2
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "ACC" + uuid.substring(0, 6);
    }

    private AccountDto.AccountResponse mapToResponse(Account account) {
        return AccountDto.AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .customerName(account.getCustomerName())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .build();
    }
}
