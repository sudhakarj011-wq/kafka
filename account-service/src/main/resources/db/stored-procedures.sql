-- ============================================================
-- STORED PROCEDURE: sp_transfer_funds
-- Purpose: Atomically transfers funds between two accounts
--          at the DATABASE level, ensuring full ACID compliance.
--
-- INTERVIEW TIP:
-- Q: Why use a Stored Procedure for a bank transfer?
-- A: A transfer requires TWO UPDATE statements (debit + credit).
--    If the app crashes after debit but before credit, money
--    disappears! A Stored Procedure wraps both in ONE atomic
--    transaction at the DB server itself, making it safe even
--    if the application layer crashes mid-operation.
--
-- Q: What is the difference between @Transactional (Spring) and
--    this Stored Procedure?
-- A: @Transactional manages transaction at the APPLICATION layer.
--    If the JVM crashes, the transaction may not rollback properly.
--    A DB Stored Procedure transaction is managed at the DATABASE
--    layer — more reliable for critical financial operations.
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_transfer_funds$$

CREATE PROCEDURE sp_transfer_funds(
    IN  p_from_account  VARCHAR(20),
    IN  p_to_account    VARCHAR(20),
    IN  p_amount        DECIMAL(15, 2),
    OUT p_status        VARCHAR(50),
    OUT p_message       VARCHAR(255)
)
BEGIN
    -- Local variables to hold current balances
    DECLARE v_from_balance  DECIMAL(15, 2) DEFAULT 0;
    DECLARE v_to_exists     INT DEFAULT 0;

    -- Error handler: on any SQL error, rollback and set failure status
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status  = 'FAILED';
        SET p_message = 'Transaction failed due to a database error. Rolled back.';
    END;

    -- ---- Validation 1: Amount must be positive ----
    IF p_amount <= 0 THEN
        SET p_status  = 'INVALID_AMOUNT';
        SET p_message = 'Transfer amount must be greater than zero.';
        LEAVE sp_transfer_funds; -- Exit the procedure
    END IF;

    -- ---- Validation 2: Source and destination must be different ----
    IF p_from_account = p_to_account THEN
        SET p_status  = 'SAME_ACCOUNT';
        SET p_message = 'Source and destination accounts cannot be the same.';
        LEAVE sp_transfer_funds;
    END IF;

    START TRANSACTION;

        -- ---- Validation 3: Check source account balance ----
        -- FOR UPDATE locks the row to prevent concurrent modification
        SELECT balance INTO v_from_balance
        FROM accounts
        WHERE account_number = p_from_account AND status = 'ACTIVE'
        FOR UPDATE;

        IF v_from_balance IS NULL THEN
            SET p_status  = 'ACCOUNT_NOT_FOUND';
            SET p_message = CONCAT('Source account ', p_from_account, ' not found or inactive.');
            ROLLBACK;
            LEAVE sp_transfer_funds;
        END IF;

        IF v_from_balance < p_amount THEN
            SET p_status  = 'INSUFFICIENT_FUNDS';
            SET p_message = CONCAT('Insufficient balance. Available: ', v_from_balance, ', Required: ', p_amount);
            ROLLBACK;
            LEAVE sp_transfer_funds;
        END IF;

        -- ---- Validation 4: Check destination account exists ----
        SELECT COUNT(*) INTO v_to_exists
        FROM accounts
        WHERE account_number = p_to_account AND status = 'ACTIVE';

        IF v_to_exists = 0 THEN
            SET p_status  = 'DESTINATION_NOT_FOUND';
            SET p_message = CONCAT('Destination account ', p_to_account, ' not found or inactive.');
            ROLLBACK;
            LEAVE sp_transfer_funds;
        END IF;

        -- ---- ATOMIC DEBIT + CREDIT ----
        UPDATE accounts
        SET balance = balance - p_amount, updated_at = NOW()
        WHERE account_number = p_from_account;

        UPDATE accounts
        SET balance = balance + p_amount, updated_at = NOW()
        WHERE account_number = p_to_account;

    COMMIT;

    -- Success
    SET p_status  = 'SUCCESS';
    SET p_message = CONCAT('Successfully transferred ', p_amount, ' from ', p_from_account, ' to ', p_to_account);

END$$

DELIMITER ;
