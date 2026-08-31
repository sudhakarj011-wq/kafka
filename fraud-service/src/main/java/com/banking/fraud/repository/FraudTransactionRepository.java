package com.banking.fraud.repository;

import com.banking.fraud.entity.FraudTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudTransactionRepository extends JpaRepository<FraudTransaction, Long> {
    boolean existsByTransactionId(String transactionId);
}
