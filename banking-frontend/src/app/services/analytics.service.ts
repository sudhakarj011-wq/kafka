import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PaymentSummary {
  summaryDate: string;
  totalTransactions: number;
  totalVolume: number;
  lastUpdatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) { }

  getTodaySummary(): Observable<PaymentSummary> {
    return this.http.get<PaymentSummary>(`${this.apiUrl}/summary`);
  }
}
