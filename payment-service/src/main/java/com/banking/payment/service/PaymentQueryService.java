package com.banking.payment.service;

import com.banking.payment.dto.PaymentDto;
import com.banking.payment.entity.Transaction;
import com.banking.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Query Service
 * Handles data retrieval operations involving transactions.
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PaymentDto.TransactionResponse getTransaction(String transactionId) {
        Transaction t = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return mapToResponse(t);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.TransactionResponse> getTransactionHistory(String accountNumber) {
        return transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(accountNumber, accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentDto.TransactionResponse mapToResponse(Transaction t) {
        return PaymentDto.TransactionResponse.builder()
                .id(t.getId())
                .transactionId(t.getTransactionId())
                .fromAccount(t.getFromAccount())
                .toAccount(t.getToAccount())
                .amount(t.getAmount())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
