package com.banking.payment.repository;

import com.banking.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository.
 *
 * Spring Data JPA auto-generates SQL from method names.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByFromAccountOrderByCreatedAtDesc(String fromAccount);

    List<Transaction> findByToAccountOrderByCreatedAtDesc(String toAccount);

    // Get all transactions for an account (as sender OR receiver)
    List<Transaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount, String toAccount);

    long countByFromAccount(String fromAccount);
}
