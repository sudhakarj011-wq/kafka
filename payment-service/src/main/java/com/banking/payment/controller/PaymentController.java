package com.banking.payment.controller;

import com.banking.payment.dto.PaymentDto;
import com.banking.payment.service.PaymentCommandService;
import com.banking.payment.service.PaymentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment Controller — REST API Layer (CQRS enabled)
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/transfer")
    public ResponseEntity<PaymentDto.TransferResponse> transfer(
            @Valid @RequestBody PaymentDto.TransferRequest request) {

        log.info("API: Transfer request ₹{} from {} to {}",
                request.getAmount(), request.getFromAccount(), request.getToAccount());
        PaymentDto.TransferResponse response = paymentCommandService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer/saga")
    public ResponseEntity<PaymentDto.TransferResponse> transferSaga(
            @Valid @RequestBody PaymentDto.TransferRequest request) {

        log.info("API [SAGA]: Async Transfer request ₹{} from {} to {}",
                request.getAmount(), request.getFromAccount(), request.getToAccount());
        PaymentDto.TransferResponse response = paymentCommandService.transferSaga(request);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentDto.TransactionResponse> getTransaction(
            @PathVariable String transactionId) {

        log.info("API: Get transaction: {}", transactionId);
        return ResponseEntity.ok(paymentQueryService.getTransaction(transactionId));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<PaymentDto.TransactionResponse>> getHistory(
            @PathVariable String accountNumber) {

        log.info("API: Get transaction history for: {}", accountNumber);
        return ResponseEntity.ok(paymentQueryService.getTransactionHistory(accountNumber));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running ✓ | Kafka Producer: payment-events (CQRS)");
    }
}
