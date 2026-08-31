import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

export interface AccountInfo {
  accountNumber: string;
  balance: number;
  customerName: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private paymentApi = `${environment.apiUrl}/payments`;
  private accountApi = `${environment.apiUrl}/accounts`;

  constructor(private http: HttpClient) { }

  transferMoney(request: TransferRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.paymentApi}/transfer`, request);
  }

  getAccountInfo(accountNumber: string): Observable<AccountInfo> {
    return this.http.get<AccountInfo>(`${this.accountApi}/${accountNumber}`);
  }
  
  getTransactionHistory(accountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.paymentApi}/history/${accountNumber}`);
  }
}
