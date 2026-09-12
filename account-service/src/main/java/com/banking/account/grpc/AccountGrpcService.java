package com.banking.account.grpc;

import com.banking.account.dto.AccountDto;
import com.banking.account.exception.AccountException;
import com.banking.account.service.AccountService;
import com.banking.grpc.AccountResponse;
import com.banking.grpc.AccountServiceGrpc;
import com.banking.grpc.BalanceUpdateRequest;
import com.banking.grpc.GetAccountRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;

/**
 * Account gRPC Service — gRPC Server Implementation
 *
 * ======================== gRPC SERVER EXPLAINED ========================
 *
 * INTERVIEW TIP — How does gRPC server work in Spring Boot?
 *
 * 1. @GrpcService annotation (from grpc-server-spring-boot-starter):
 *    - Registers this class as a gRPC service handler
 *    - Netty gRPC server is started automatically on port 9090
 *    - No need to manually create ServerBuilder or start server
 *
 * 2. AccountServiceGrpc.AccountServiceImplBase (auto-generated from account.proto):
 *    - Contains one method per RPC defined in the .proto file
 *    - By default, each method returns UNIMPLEMENTED status
 *    - We override ONLY the methods we need
 *
 * 3. StreamObserver<T>:
 *    - gRPC's callback mechanism for sending responses
 *    - onNext(response)    → sends the response body
 *    - onError(throwable)  → sends error status to client
 *    - onCompleted()       → signals end of response stream
 *    - For unary RPC: onNext() exactly once, then onCompleted()
 *
 * DESIGN PRINCIPLE (Single Responsibility / No Duplication):
 * This class does NOT contain any business logic.
 * It DELEGATES everything to the existing AccountService.
 * gRPC layer is just a "transport adapter" — like a Controller for REST.
 *
 * EXISTING CODE UNTOUCHED:
 * - AccountService.java         → NOT modified
 * - AccountController.java      → NOT modified (REST still works)
 * - AccountRepository.java      → NOT modified
 * - Kafka consumers             → NOT modified
 *
 * ======================== HTTP/2 vs HTTP/1.1 ========================
 * REST (OpenFeign) uses HTTP/1.1:
 *   - One request per TCP connection (or keep-alive with limits)
 *   - Text-based headers (verbose)
 *   - Body is JSON (human-readable but large)
 *
 * gRPC uses HTTP/2:
 *   - Multiple requests multiplexed on ONE TCP connection
 *   - Binary compressed headers (HPACK)
 *   - Body is Protobuf (binary, 5-10x smaller than JSON)
 *   - Result: lower latency, higher throughput for internal calls
 * ====================================================================
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    // Inject existing AccountService — zero business logic duplication
    private final AccountService accountService;

    /**
     * GetAccount RPC — returns account details by account number.
     *
     * INTERVIEW TIP — Unary RPC flow:
     * 1. Client sends GetAccountRequest (proto binary over HTTP/2)
     * 2. Netty deserializes it into GetAccountRequest Java object
     * 3. This method is called
     * 4. We call accountService.getAccountByNumber() (existing logic)
     * 5. We map AccountDto.AccountResponse → AccountResponse (proto)
     * 6. responseObserver.onNext(response) sends it back
     * 7. responseObserver.onCompleted() closes the call
     */
    @Override
    public void getAccount(GetAccountRequest request,
                           StreamObserver<AccountResponse> responseObserver) {
        log.info("gRPC GetAccount called for: {}", request.getAccountNumber());
        try {
            AccountDto.AccountResponse dto =
                    accountService.getAccountByNumber(request.getAccountNumber());

            responseObserver.onNext(toProto(dto));
            responseObserver.onCompleted();

        } catch (AccountException.AccountNotFoundException e) {
            log.warn("gRPC GetAccount — Account not found: {}", request.getAccountNumber());
            /*
             * INTERVIEW TIP — gRPC Error Handling:
             * gRPC has its own status codes (like HTTP status codes):
             *   Status.NOT_FOUND       → 404 equivalent
             *   Status.INVALID_ARGUMENT → 400 equivalent
             *   Status.INTERNAL        → 500 equivalent
             *   Status.UNAUTHENTICATED → 401 equivalent
             *   Status.PERMISSION_DENIED → 403 equivalent
             *
             * Client receives StatusRuntimeException with this status code.
             * withDescription() adds a message (like HTTP response body).
             * asRuntimeException() converts Status → StreamObserver-compatible exception.
             */
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Account not found: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetAccount — Unexpected error: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * DebitAccount RPC — deducts amount from account.
     * Replaces: accountServiceClient.debitAccount() in PaymentCommandService (Feign).
     */
    @Override
    public void debitAccount(BalanceUpdateRequest request,
                             StreamObserver<AccountResponse> responseObserver) {
        log.info("gRPC DebitAccount called — account: {}, amount: ₹{}",
                request.getAccountNumber(), request.getAmount());
        try {
            AccountDto.BalanceUpdateRequest dto = toBalanceUpdateDto(request.getAmount());
            AccountDto.AccountResponse result =
                    accountService.debitAccount(request.getAccountNumber(), dto);

            responseObserver.onNext(toProto(result));
            responseObserver.onCompleted();

        } catch (AccountException.AccountNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Account not found: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (AccountException.AccountInactiveException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Account is inactive: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (AccountException.InsufficientBalanceException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Insufficient balance for account: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC DebitAccount — Unexpected error: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * CreditAccount RPC — adds amount to account.
     * Replaces: accountServiceClient.creditAccount() in PaymentCommandService (Feign).
     */
    @Override
    public void creditAccount(BalanceUpdateRequest request,
                              StreamObserver<AccountResponse> responseObserver) {
        log.info("gRPC CreditAccount called — account: {}, amount: ₹{}",
                request.getAccountNumber(), request.getAmount());
        try {
            AccountDto.BalanceUpdateRequest dto = toBalanceUpdateDto(request.getAmount());
            AccountDto.AccountResponse result =
                    accountService.creditAccount(request.getAccountNumber(), dto);

            responseObserver.onNext(toProto(result));
            responseObserver.onCompleted();

        } catch (AccountException.AccountNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Account not found: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (AccountException.AccountInactiveException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Account is inactive: " + request.getAccountNumber())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC CreditAccount — Unexpected error: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ======================== PRIVATE HELPER METHODS ========================

    /**
     * Maps AccountDto.AccountResponse (existing Java DTO) → AccountResponse (proto-generated class).
     *
     * INTERVIEW TIP — Why do we need this mapping?
     * The proto-generated AccountResponse and our existing AccountDto.AccountResponse
     * are two separate classes. We bridge them here (in the gRPC layer only)
     * so the business logic (AccountService) stays oblivious to gRPC.
     *
     * This is the ANTI-CORRUPTION LAYER pattern from DDD:
     * — The proto world and the domain world are kept separate.
     */
    private AccountResponse toProto(AccountDto.AccountResponse dto) {
        return AccountResponse.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : 0L)
                .setAccountNumber(dto.getAccountNumber())
                .setCustomerId(dto.getCustomerId())
                .setCustomerName(dto.getCustomerName())
                // BigDecimal → double for proto transport
                // INTERVIEW TIP: In production, use string to avoid double precision issues
                .setBalance(dto.getBalance() != null ? dto.getBalance().doubleValue() : 0.0)
                .setStatus(dto.getStatus())
                .build();
    }

    /**
     * Maps proto double amount → AccountDto.BalanceUpdateRequest (existing DTO).
     * Converts double back to BigDecimal using string to preserve precision.
     */
    private AccountDto.BalanceUpdateRequest toBalanceUpdateDto(double amount) {
        return AccountDto.BalanceUpdateRequest.builder()
                .amount(BigDecimal.valueOf(amount))
                .build();
    }
}
