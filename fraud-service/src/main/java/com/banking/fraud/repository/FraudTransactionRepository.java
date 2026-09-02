package com.banking.fraud.repository;

import com.banking.fraud.entity.FraudTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudTransactionRepository extends JpaRepository<FraudTransaction, Long> {
    boolean existsByTransactionId(String transactionId);
    Optional<FraudTransaction> findByTransactionId(String transactionId);
    List<FraudTransaction> findByRiskLevel(FraudTransaction.RiskLevel riskLevel);
}
