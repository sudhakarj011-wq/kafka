import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { AuthService } from '../../services/auth.service';
import { AsyncPipe, CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [NgFor, NgIf, AsyncPipe, DatePipe, CurrencyPipe],
  template: `
    <div class="container fade-in">
      <div class="header">
        <h1>Transaction History</h1>
        <p class="subtitle">Your recent transfers and payments</p>
      </div>

      <div class="glass-card table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Txn ID</th>
              <th>Type</th>
              <th>Account</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Date & Time</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let txn of history$ | async">
              <td class="txn-id">{{ txn.transactionId.substring(0,8) }}...</td>
              <td>
                <span class="badge" 
                      [class.badge-success]="txn.toAccount === accountNum"
                      [class.badge-warning]="txn.fromAccount === accountNum">
                  {{ txn.toAccount === accountNum ? 'CREDIT' : 'DEBIT' }}
                </span>
              </td>
              <td class="account">{{ txn.toAccount === accountNum ? txn.fromAccount : txn.toAccount }}</td>
              <td class="amount" [class.text-success]="txn.toAccount === accountNum">
                {{ txn.toAccount === accountNum ? '+' : '-' }}{{ txn.amount | currency:'INR':'symbol' }}
              </td>
              <td>
                <span class="badge" 
                      [class.badge-success]="txn.status === 'SUCCESS'"
                      [class.badge-danger]="txn.status === 'FAILED'">
                  {{ txn.status }}
                </span>
              </td>
              <td class="date">{{ txn.timestamp | date:'short' }}</td>
            </tr>
          </tbody>
        </table>
        
        <div *ngIf="(history$ | async)?.length === 0" class="empty-state">
          No transactions found.
        </div>
      </div>
    </div>
  `,
  styles: [`
    .header { margin-bottom: 2rem; }
    .subtitle { color: var(--text-secondary); margin-top: 0.5rem; }
    
    .table-container { 
      padding: 0;
      overflow: hidden;
    }
    .table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    th {
      padding: 1.5rem;
      background: rgba(0,0,0,0.2);
      color: var(--text-secondary);
      font-weight: 500;
      font-size: 0.9rem;
      border-bottom: 1px solid var(--glass-border);
    }
    td {
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid rgba(255,255,255,0.05);
      vertical-align: middle;
    }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: rgba(255,255,255,0.02); }
    
    .txn-id, .account {
      font-family: monospace;
      color: var(--text-secondary);
    }
    .amount {
      font-weight: 600;
      font-family: var(--font-heading);
    }
    .text-success {
      color: var(--success);
    }
    .date {
      color: var(--text-secondary);
      font-size: 0.9rem;
    }
    .empty-state {
      padding: 3rem;
      text-align: center;
      color: var(--text-secondary);
    }
    
    .fade-in {
      animation: fadeIn 0.4s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class HistoryComponent implements OnInit {
  history$: Observable<any[]> | null = null;
  accountNum = '';

  constructor(
    private paymentService: PaymentService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    const custId = this.auth.getCustomerId();
    // Using mapping logic from dashboard
    this.accountNum = custId === 'CUST001' ? 'ACC1001' : (custId === 'CUST002' ? 'ACC1002' : 'ACC1001');
    
    this.history$ = this.paymentService.getTransactionHistory(this.accountNum);
  }
}
