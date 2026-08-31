package com.banking.account.repository;

import com.banking.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Account Repository — Spring Data JPA
 *
 * Spring Data JPA automatically generates SQL queries from method names.
 * No need to write SQL manually for common operations.
 *
 * INTERVIEW TIP:
 * Q: How does findByAccountNumber work without any SQL?
 * A: Spring Data JPA parses the method name at startup:
 *    find + By + AccountNumber → SELECT * FROM accounts WHERE account_number = ?
 *    This is called "derived queries" or "query derivation".
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number.
     * SQL: SELECT * FROM accounts WHERE account_number = ?
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by customer ID.
     * SQL: SELECT * FROM accounts WHERE customer_id = ?
     */
    Optional<Account> findByCustomerId(String customerId);

    /**
     * Check if account number already exists (for uniqueness validation).
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Custom JPQL query to update balance directly.
     * Using @Modifying + @Query for performance — avoids fetching
     * the entity just to update one field.
     *
     * INTERVIEW TIP:
     * Q: Why use @Modifying with @Query?
     * A: For bulk/targeted updates, it's more efficient than:
     *    1. Load entity → 2. Modify → 3. Save (3 operations)
     *    vs. direct UPDATE query (1 operation).
     */
    @Modifying
    @Query("UPDATE Account a SET a.balance = :balance WHERE a.accountNumber = :accountNumber")
    int updateBalance(@Param("accountNumber") String accountNumber,
                      @Param("balance") BigDecimal balance);
}
