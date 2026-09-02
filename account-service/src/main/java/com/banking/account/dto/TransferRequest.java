package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Stored Procedure based fund transfer request.
 *
 * INTERVIEW TIP:
 * Q: Why a separate DTO instead of reusing existing ones?
 * A: The existing AccountDto is for account operations.
 *    This DTO is specific to the transfer use case and follows
 *    the Single Responsibility Principle (SRP) — each class
 *    has one clearly defined purpose.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    /** Account number from which money will be debited */
    private String fromAccount;

    /** Account number to which money will be credited */
    private String toAccount;

    /** Amount to transfer — must be > 0 */
    private BigDecimal amount;

    /** Optional: reason/description for the transfer */
    private String remark;
}
