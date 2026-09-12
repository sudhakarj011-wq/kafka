import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RazorpayOrderRequest {
  amount: number;
  accountNumber: string;
  currency?: string;
}

export interface RazorpayOrderResponse {
  orderId: string;
  amount: number;
  currency: string;
  keyId: string;
}

@Injectable({
  providedIn: 'root'
})
export class RazorpayService {
  private razorpayApi = `${environment.apiUrl}/razorpay`;

  constructor(private http: HttpClient) {}

  createOrder(request: RazorpayOrderRequest): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.razorpayApi}/order`, request);
  }
}
