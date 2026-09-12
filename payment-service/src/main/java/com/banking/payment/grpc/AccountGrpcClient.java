package com.banking.payment.grpc;

import com.banking.grpc.AccountResponse;
import com.banking.grpc.AccountServiceGrpc;
import com.banking.grpc.BalanceUpdateRequest;
import com.banking.grpc.GetAccountRequest;
import com.banking.payment.dto.AccountClientDto;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.math.BigDecimal;

/**
 * Account gRPC Client — Wraps gRPC calls to account-service
 *
 * ======================== gRPC CLIENT EXPLAINED ========================
 *
 * INTERVIEW TIP — How does @GrpcClient work?
 * @GrpcClient("account-service") tells grpc-client-spring-boot-starter:
 * 1. Look up the configuration: grpc.client.account-service.address in application.yml
 * 2. Create a ManagedChannel (HTTP/2 connection pool) to that address
 * 3. Create a BlockingStub wrapping that channel
 * 4. Inject it here
 *
 * The channel is SHARED and REUSED across all requests (connection pooling).
 * This is far more efficient than HTTP/1.1 (Feign) which creates new
 * connections frequently.
 *
 * INTERVIEW TIP — BlockingStub vs AsyncStub vs FutureStub:
 * - BlockingStub (used here): thread blocks until response — simplest, matches Feign pattern
 * - FutureStub: returns ListenableFuture — integrate with CompletableFuture
 * - AsyncStub: callback-based via StreamObserver — for streaming RPCs
 *
 * We chose BlockingStub because:
 * 1. Existing PaymentCommandService uses @Transactional — synchronous pattern
 * 2. Balance updates must be sequential (debit then credit)
 * 3. No streaming needed for these operations
 *
 * DESIGN PRINCIPLE (Wrapper/Facade Pattern):
 * This class is the ONLY place in payment-service that knows about gRPC.
 * It returns the SAME AccountClientDto types that Feign used.
 * → PaymentCommandService doesn't know it's calling gRPC (decoupled!)
 * → If tomorrow you swap gRPC back to REST, only this class changes.
 *
 * INTERVIEW TIP — Why return AccountClientDto and not the proto AccountResponse?
 * - AccountClientDto is the domain DTO of payment-service
 * - proto AccountResponse is a transport DTO (generated code)
 * - Mixing them couples your business logic to the transport layer
 * - This class acts as an ANTI-CORRUPTION LAYER
 * ======================================================================
 */
@Component
@Slf4j
public class AccountGrpcClient {

    /**
     * @GrpcClient("account-service"):
     * "account-service" must match the key in application.yml:
     *   grpc.client.account-service.address = static://localhost:9090
     *
     * grpc-client-spring-boot-starter injects the AccountServiceGrpc.AccountServiceBlockingStub
     * auto-generated from account.proto.
     */
    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    /**
     * Get account details by account number.
     * Replaces: accountServiceClient.getAccount(accountNumber) [Feign → REST GET]
     *
     * Sends GetAccountRequest → receives AccountResponse via gRPC (HTTP/2 + Protobuf)
     * Returns: AccountClientDto.AccountResponse — same type Feign returned
     */
    @CircuitBreaker(name = "accountGrpcClient", fallbackMethod = "fallbackGetAccount")
    public AccountClientDto.AccountResponse getAccount(String accountNumber) {
        log.info("gRPC → GetAccount: {}", accountNumber);
        try {
            GetAccountRequest request = GetAccountRequest.newBuilder()
                    .setAccountNumber(accountNumber)
                    .build();

            AccountResponse response = accountStub.getAccount(request);
            return toAccountClientDto(response);

        } catch (StatusRuntimeException e) {
            /*
             * INTERVIEW TIP — StatusRuntimeException:
             * This is the gRPC equivalent of Feign's FeignException.
             * e.getStatus().getCode() gives the gRPC status code:
             *   Status.Code.NOT_FOUND          → account doesn't exist
             *   Status.Code.FAILED_PRECONDITION → business rule violation
             *   Status.Code.UNAVAILABLE         → server down / network issue
             *   Status.Code.DEADLINE_EXCEEDED   → timeout
             *
             * e.getStatus().getDescription() gives the error message.
             */
            log.error("gRPC GetAccount failed for {}: {} - {}",
                    accountNumber, e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("Account Service gRPC error: " + e.getStatus().getDescription(), e);
        }
    }

    /**
     * Debit amount from account.
     * Replaces: accountServiceClient.debitAccount(accountNumber, request) [Feign → REST PUT]
     */
    @CircuitBreaker(name = "accountGrpcClient", fallbackMethod = "fallbackDebitAccount")
    public AccountClientDto.AccountResponse debitAccount(String accountNumber, BigDecimal amount) {
        log.info("gRPC → DebitAccount: {} | ₹{}", accountNumber, amount);
        try {
            BalanceUpdateRequest request = BalanceUpdateRequest.newBuilder()
                    .setAccountNumber(accountNumber)
                    .setAmount(amount.doubleValue())
                    .build();

            AccountResponse response = accountStub.debitAccount(request);
            return toAccountClientDto(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC DebitAccount failed for {}: {} - {}",
                    accountNumber, e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("Debit failed: " + e.getStatus().getDescription(), e);
        }
    }

    /**
     * Credit amount to account.
     * Replaces: accountServiceClient.creditAccount(accountNumber, request) [Feign → REST PUT]
     */
    @CircuitBreaker(name = "accountGrpcClient", fallbackMethod = "fallbackCreditAccount")
    public AccountClientDto.AccountResponse creditAccount(String accountNumber, BigDecimal amount) {
        log.info("gRPC → CreditAccount: {} | ₹{}", accountNumber, amount);
        try {
            BalanceUpdateRequest request = BalanceUpdateRequest.newBuilder()
                    .setAccountNumber(accountNumber)
                    .setAmount(amount.doubleValue())
                    .build();

            AccountResponse response = accountStub.creditAccount(request);
            return toAccountClientDto(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC CreditAccount failed for {}: {} - {}",
                    accountNumber, e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("Credit failed: " + e.getStatus().getDescription(), e);
        }
    }

    // ======================== FALLBACK METHODS ========================

    public AccountClientDto.AccountResponse fallbackGetAccount(String accountNumber, Exception e) {
        log.warn("Circuit Breaker FALLBACK: getAccount failed for {}. Error: {}", accountNumber, e.getMessage());
        throw new RuntimeException("Account Service is currently unavailable. Please try again later.", e);
    }

    public AccountClientDto.AccountResponse fallbackDebitAccount(String accountNumber, BigDecimal amount, Exception e) {
        log.warn("Circuit Breaker FALLBACK: debitAccount failed for {}. Error: {}", accountNumber, e.getMessage());
        throw new RuntimeException("Account Service is currently unavailable. Transaction aborted.", e);
    }

    public AccountClientDto.AccountResponse fallbackCreditAccount(String accountNumber, BigDecimal amount, Exception e) {
        log.warn("Circuit Breaker FALLBACK: creditAccount failed for {}. Error: {}", accountNumber, e.getMessage());
        throw new RuntimeException("Account Service is currently unavailable. Transaction aborted.", e);
    }

    // ======================== PRIVATE HELPER ========================

    /**
     * Maps proto AccountResponse → AccountClientDto.AccountResponse (existing DTO).
     *
     * INTERVIEW TIP — Anti-corruption layer:
     * This keeps the gRPC transport concern isolated in this class.
     * PaymentCommandService only sees AccountClientDto — oblivious to transport.
     */
    private AccountClientDto.AccountResponse toAccountClientDto(AccountResponse proto) {
        return AccountClientDto.AccountResponse.builder()
                .id(proto.getId())
                .accountNumber(proto.getAccountNumber())
                .customerId(proto.getCustomerId())
                .customerName(proto.getCustomerName())
                // double → BigDecimal via String to preserve precision
                .balance(BigDecimal.valueOf(proto.getBalance()))
                .status(proto.getStatus())
                .build();
    }
}
