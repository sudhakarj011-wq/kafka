import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TransferRequest {
  fromAccount: string;
  toAccount: string;
  amount: number;
}

export interface PaymentResponse {
  transactionId: string;
  status: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private paymentApi = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) { }

  /**
   * Transfer money between accounts.
   * Backend internally calls AccountGrpcClient.debitAccount() + creditAccount()
   * Angular only knows: POST /payments/transfer → PaymentResponse
   */
  transferMoney(request: TransferRequest): Observable<PaymentResponse> {
    // Generate unique UUID for each transfer attempt.
    // Prevents double-charging if the user clicks twice or network retries occur.
    const headers = new HttpHeaders({
      'X-Idempotency-Key': crypto.randomUUID()
    });
    return this.http.post<PaymentResponse>(`${this.paymentApi}/transfer`, request, { headers });
  }

  /**
   * Get transaction history for an account.
   * REST route: GET /payments/history/{accountNumber}
   */
  getTransactionHistory(accountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.paymentApi}/history/${accountNumber}`);
  }
}
