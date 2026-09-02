package com.banking.account.controller;

import com.banking.account.dto.TransferRequest;
import com.banking.account.dto.TransferResponse;
import com.banking.account.service.StoredProcedureTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * StoredProcedureController
 *
 * Exposes a NEW endpoint: POST /api/accounts/transfer/sp
 *
 * ============================================================
 * DESIGN DECISION: Why a separate controller?
 * ============================================================
 * The existing AccountController handles CRUD operations for
 * accounts (create, get, debit, credit).
 *
 * This controller is specifically for demonstrating how
 * Stored Procedure-based transfers work — a separate concern.
 * Separating them keeps code clean and follows the principle:
 * ONE controller = ONE logical resource area.
 *
 * INTERVIEW TIP:
 * Q: What HTTP method should be used for a transfer? POST or PUT?
 * A: POST — because a transfer is NOT idempotent.
 *    Calling it twice transfers money TWICE.
 *    PUT is idempotent (same result on repeated calls).
 *    POST is for actions/operations, PUT is for updates.
 * ============================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")   // Consistent base path with AccountController
@RequiredArgsConstructor
public class StoredProcedureController {

    private final StoredProcedureTransferService transferService;

    /**
     * POST /api/accounts/transfer/sp
     *
     * Transfers funds between two accounts using a MySQL Stored Procedure.
     * The procedure handles ACID transactions at the database level.
     *
     * Request Body:
     * {
     *   "fromAccount": "ACC001",
     *   "toAccount":   "ACC002",
     *   "amount":      5000.00,
     *   "remark":      "Salary credit"
     * }
     *
     * Response:
     * {
     *   "status":  "SUCCESS",
     *   "message": "Successfully transferred 5000.00 from ACC001 to ACC002",
     *   "success": true
     * }
     */
    @PostMapping("/transfer/sp")
    public ResponseEntity<TransferResponse> transferViaSP(
            @RequestBody TransferRequest request) {

        log.info("Received SP transfer request: {} -> {} | ₹{}",
                request.getFromAccount(), request.getToAccount(), request.getAmount());

        // Basic validation before hitting the database
        if (request.getFromAccount() == null || request.getToAccount() == null
                || request.getAmount() == null) {
            return ResponseEntity.badRequest()
                    .body(TransferResponse.builder()
                            .status("INVALID_REQUEST")
                            .message("fromAccount, toAccount and amount are required fields.")
                            .success(false)
                            .build());
        }

        // Execute via Stored Procedure
        TransferResponse response = transferService.executeTransfer(request);

        // Return 200 OK for success, 400 Bad Request for business failures
        HttpStatus httpStatus = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(httpStatus).body(response);
    }
}
