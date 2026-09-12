import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * AccountService — Angular-side wrapper for gRPC-backed Account endpoints.
 *
 * ======================== gRPC Angular Integration ========================
 *
 * INTERVIEW TIP — Why does Angular use REST here if backend uses gRPC?
 *
 * Browsers CANNOT speak raw gRPC / HTTP/2 directly. The browser's
 * XMLHttpRequest and Fetch APIs do not expose HTTP/2 trailing headers
 * that gRPC requires. Two valid approaches exist in real projects:
 *
 * APPROACH 1 (Used here — REST Gateway pattern):
 *   Angular → REST (JSON / HTTP 1.1) → Spring Boot (REST Controller)
 *                                         → gRPC (Protobuf / HTTP/2)
 *                                         → account-service
 *   Pros: Simple, no proxy needed, same security/JWT model.
 *
 * APPROACH 2 (Advanced — gRPC-Web + Envoy Proxy):
 *   Angular (grpc-web npm) → grpc-web format → Envoy Proxy
 *                                               → gRPC → account-service
 *   Pros: End-to-end Protobuf, no REST serialization overhead.
 *   Cons: Requires Envoy sidecar, browser .proto compilation step.
 *
 * Design Decision: Approach 1 is used because:
 *   - No extra infrastructure (Envoy) required
 *   - Spring Boot API Gateway already acts as the REST-to-gRPC bridge
 *   - Angular remains transport-agnostic (if backend swaps gRPC to anything
 *     else, Angular code is ZERO impact)
 *
 * SINGLE RESPONSIBILITY:
 *   This service is ONLY for account-related calls.
 *   PaymentService handles payments. AuthService handles auth.
 *   No duplication of interfaces or methods across services.
 * =========================================================================
 */

// ─────────────────────────────────────────────
// DTOs — Mirror of backend gRPC proto messages
// ─────────────────────────────────────────────

/**
 * Maps to proto AccountResponse (generated from account.proto).
 * Returned by getAccount, debitAccount, creditAccount.
 */
export interface AccountInfo {
  id: number;
  accountNumber: string;
  customerId: string;
  customerName: string;
  balance: number;
  status: string;
}

/**
 * Maps to proto BalanceUpdateRequest (generated from account.proto).
 * Sent to debitAccount and creditAccount.
 */
export interface BalanceUpdateRequest {
  amount: number;
}

// ─────────────────────────────────────────────
// Service
// ─────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  /** Base URL for account endpoints — routed through API Gateway */
  private accountApi = `${environment.apiUrl}/accounts`;

  constructor(private http: HttpClient) {}

  /**
   * Get account details by account number.
   *
   * Maps to backend gRPC: AccountService.getAccount(GetAccountRequest)
   * REST route (API Gateway): GET /api/accounts/{accountNumber}
   *
   * INTERVIEW TIP — What happens on backend?
   * 1. API Gateway forwards to account-service REST controller.
   * 2. account-service resolves internally (direct DB call — no gRPC needed
   *    to itself). Returns AccountDto.
   *
   * Used by: DashboardComponent to show live balance.
   */
  getAccount(accountNumber: string): Observable<AccountInfo> {
    return this.http.get<AccountInfo>(`${this.accountApi}/${accountNumber}`);
  }

  /**
   * Debit (cut) amount from account.
   *
   * Maps to backend gRPC: AccountService.debitAccount(BalanceUpdateRequest)
   * REST route: PUT /api/accounts/{accountNumber}/debit
   *
   * INTERVIEW TIP — When is this called from Angular?
   * Typically NOT called directly from Angular — the transfer flow goes:
   * Angular → POST /payments/transfer → PaymentCommandService (Spring Boot)
   *   → accountGrpcClient.debitAccount() [gRPC inside backend]
   * This method exists for direct admin/test scenarios.
   */
  debitAccount(accountNumber: string, body: BalanceUpdateRequest): Observable<AccountInfo> {
    return this.http.put<AccountInfo>(`${this.accountApi}/${accountNumber}/debit`, body);
  }

  /**
   * Credit (add) amount to account.
   *
   * Maps to backend gRPC: AccountService.creditAccount(BalanceUpdateRequest)
   * REST route: PUT /api/accounts/{accountNumber}/credit
   *
   * INTERVIEW TIP — When is this called from Angular?
   * Used by: Razorpay deposit flow — after payment.captured webhook,
   * credit is done inside RazorpayService (Spring Boot) via gRPC.
   * This Angular method can be used for manual admin credit if needed.
   */
  creditAccount(accountNumber: string, body: BalanceUpdateRequest): Observable<AccountInfo> {
    return this.http.put<AccountInfo>(`${this.accountApi}/${accountNumber}/credit`, body);
  }

  /**
   * Get account by Customer ID.
   * REST route: GET /api/accounts/by-customer/{customerId}
   * Used by: AuthService at login to resolve customerId → accountNumber.
   */
  getAccountByCustomerId(customerId: string): Observable<AccountInfo> {
    return this.http.get<AccountInfo>(`${this.accountApi}/by-customer/${customerId}`);
  }
}
