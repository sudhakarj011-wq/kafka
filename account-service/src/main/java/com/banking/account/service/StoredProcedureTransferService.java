package com.banking.account.service;

import com.banking.account.dto.TransferRequest;
import com.banking.account.dto.TransferResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * StoredProcedureTransferService
 *
 * Executes the MySQL Stored Procedure `sp_transfer_funds`
 * using JPA's StoredProcedureQuery API.
 *
 * ============================================================
 * INTERVIEW QUESTIONS & ANSWERS:
 * ============================================================
 *
 * Q1: Why a SEPARATE service class, not modifying AccountService?
 * A1: Open/Closed Principle (OCP) from SOLID:
 *     Classes should be OPEN for extension, CLOSED for modification.
 *     Adding a new service class extends functionality
 *     without risking breaking the existing AccountService.
 *
 * Q2: What is EntityManager in Spring JPA?
 * A2: EntityManager is the JPA interface that manages the
 *     persistence context. It is the lower-level API compared
 *     to Spring Data Repositories and is needed for operations
 *     like calling Stored Procedures and Native Queries.
 *
 * Q3: How does createStoredProcedureQuery differ from @Procedure?
 * A3: @Procedure annotation (on Repository) is cleaner but
 *     requires the entity to be annotated with
 *     @NamedStoredProcedureQuery — which would pollute the
 *     Account entity. createStoredProcedureQuery() on EntityManager
 *     keeps the entity class clean and is more flexible for
 *     procedures with OUT parameters.
 *
 * Q4: What are IN and OUT parameters in a Stored Procedure?
 * A4: IN  = Input sent from Java to database (like method args)
 *     OUT = Output returned from database to Java (like return value)
 *     INOUT = Both
 *
 * Q5: How is this different from the Saga-based transfer?
 * A5: Saga (Kafka) = Asynchronous, distributed, eventual consistency
 *     Stored Procedure = Synchronous, single-DB, strong consistency
 *     Use Stored Procedure when both accounts are in the SAME database.
 *     Use Saga when accounts span MULTIPLE microservices/databases.
 * ============================================================
 */
@Slf4j
@Service
public class StoredProcedureTransferService {

    // EntityManager gives direct access to JPA persistence context
    // PersistenceContext injects it automatically (Spring manages lifecycle)
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Executes the `sp_transfer_funds` MySQL Stored Procedure.
     *
     * @param request contains fromAccount, toAccount, amount
     * @return TransferResponse with status and message from the SP
     */
    public TransferResponse executeTransfer(TransferRequest request) {

        log.info("Initiating SP-based transfer: {} -> {} | Amount: {}",
                request.getFromAccount(), request.getToAccount(), request.getAmount());

        // Step 1: Create a reference to the stored procedure by name
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_transfer_funds");

        // Step 2: Register IN parameters (inputs sent to SP)
        query.registerStoredProcedureParameter("p_from_account", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_account", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_amount", java.math.BigDecimal.class, ParameterMode.IN);

        // Step 3: Register OUT parameters (outputs returned from SP)
        query.registerStoredProcedureParameter("p_status", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_message", String.class, ParameterMode.OUT);

        // Step 4: Set actual values for IN parameters
        query.setParameter("p_from_account", request.getFromAccount());
        query.setParameter("p_to_account", request.getToAccount());
        query.setParameter("p_amount", request.getAmount());

        // Step 5: Execute the stored procedure
        query.execute();

        // Step 6: Read OUT parameter values returned by SP
        String status  = (String) query.getOutputParameterValue("p_status");
        String message = (String) query.getOutputParameterValue("p_message");

        log.info("SP Transfer result: status={}, message={}", status, message);

        return TransferResponse.builder()
                .status(status)
                .message(message)
                .success("SUCCESS".equals(status))
                .build();
    }
}
