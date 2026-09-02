package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Stored Procedure transfer response.
 *
 * INTERVIEW TIP:
 * Q: What status codes does the SP return?
 * A: SUCCESS, INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND,
 *    DESTINATION_NOT_FOUND, INVALID_AMOUNT, SAME_ACCOUNT, FAILED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    /** Status returned by the stored procedure */
    private String status;

    /** Human-readable message from the stored procedure */
    private String message;

    /** true if status == SUCCESS */
    private boolean success;
}
