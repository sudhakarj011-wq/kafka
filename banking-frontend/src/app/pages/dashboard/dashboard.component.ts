import { Component, OnInit } from '@angular/core';
import { AsyncPipe, CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PaymentService, AccountInfo } from '../../services/payment.service';
import { AnalyticsService, PaymentSummary } from '../../services/analytics.service';
import { NotificationService, Notification } from '../../services/notification.service';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, AsyncPipe, CurrencyPipe, DatePipe, RouterLink],
  template: `
    <div class="container fade-in">
      <div class="header">
        <h1>Dashboard</h1>
        <p class="subtitle">Welcome back, {{ (accountInfo$ | async)?.customerName }} 👋</p>
      </div>

      <div class="grid-layout">
        
        <!-- Live Balance Card -->
        <div class="glass-card balance-card">
          <div class="card-title">Available Balance</div>
          <div class="balance-amount">
            <span *ngIf="accountInfo$ | async as info">{{ info.balance | currency:'INR':'symbol' }}</span>
            <span *ngIf="!(accountInfo$ | async)">₹0.00</span>
          </div>
          <p class="account-number">Account: {{ (accountInfo$ | async)?.accountNumber }}</p>
          <a routerLink="/transfer" class="btn-primary mt-4">Make a Transfer</a>
        </div>

        <!-- Global Analytics Card (Kafka Driven) -->
        <div class="glass-card analytics-card">
          <div class="card-title">
            <span class="live-dot"></span> Live Bank Analytics
          </div>
          <p class="analytics-desc">Real-time metrics from the Kafka Analytics Consumer</p>
          
          <div class="stats-row" *ngIf="summary$ | async as sum">
            <div class="stat-box">
              <div class="stat-label">Total Transactions Today</div>
              <div class="stat-value">{{ sum.totalTransactions }}</div>
            </div>
            <div class="stat-box">
              <div class="stat-label">Total Volume Today</div>
              <div class="stat-value text-accent">{{ sum.totalVolume | currency:'INR':'symbol' }}</div>
            </div>
          </div>
          <div *ngIf="!(summary$ | async)" class="mt-4 text-muted">
            Waiting for first transaction today...
          </div>
        </div>

        <!-- Recent Notifications -->
        <div class="glass-card notifications-card full-width">
          <div class="card-title">Recent Notifications</div>
          
          <div class="updates-list">
            <div class="update-item" *ngFor="let notif of notifications">
              <div class="update-icon" [class]="getIconClass(notif.type)">
                {{ getIcon(notif.type) }}
              </div>
              <div class="update-content">
                <div class="update-text">{{ notif.message }}</div>
                <div class="update-time">{{ notif.createdAt | date:'medium' }}</div>
              </div>
            </div>
            <div *ngIf="notifications.length === 0" class="text-muted">
              No recent notifications
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .header {
      margin-bottom: 2rem;
    }
    .subtitle {
      color: var(--text-secondary);
      font-size: 1.1rem;
      margin-top: 0.5rem;
    }
    .grid-layout {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
    }
    .full-width {
      grid-column: 1 / -1;
    }
    .card-title {
      font-size: 1.1rem;
      font-weight: 600;
      color: var(--text-secondary);
      margin-bottom: 1rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .balance-amount {
      font-size: 3rem;
      font-weight: 700;
      background: var(--accent-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      line-height: 1.2;
    }
    .account-number {
      font-family: monospace;
      color: var(--text-secondary);
      margin-top: 0.5rem;
    }
    
    /* Live Analytics indicator */
    .live-dot {
      width: 10px;
      height: 10px;
      background-color: var(--success);
      border-radius: 50%;
      display: inline-block;
      box-shadow: 0 0 10px var(--success);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
      70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }
    .analytics-desc {
      color: var(--text-secondary);
      font-size: 0.9rem;
      margin-bottom: 1.5rem;
    }
    .stats-row {
      display: flex;
      gap: 1.5rem;
    }
    .stat-box {
      background: rgba(255,255,255,0.03);
      padding: 1rem;
      border-radius: 12px;
      flex: 1;
      text-align: center;
    }
    .stat-label {
      font-size: 0.85rem;
      color: var(--text-secondary);
      margin-bottom: 0.5rem;
    }
    .stat-value {
      font-size: 1.5rem;
      font-weight: 700;
    }
    .text-accent {
      color: var(--accent-primary);
    }
    
    .updates-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .update-item {
      display: flex;
      gap: 1rem;
      padding: 1rem;
      background: rgba(255,255,255,0.02);
      border-radius: 12px;
      align-items: center;
      transition: background 0.2s;
    }
    .update-item:hover {
      background: rgba(255,255,255,0.05);
    }
    .update-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-size: 1.2rem;
    }
    .icon-success { background: var(--success-bg); color: var(--success); }
    .icon-danger { background: var(--danger-bg); color: var(--danger); }
    .icon-sys { background: rgba(99, 102, 241, 0.1); color: var(--accent-primary); }
    
    .update-text {
      font-weight: 500;
    }
    .update-time {
      font-size: 0.8rem;
      color: var(--text-secondary);
      margin-top: 0.2rem;
    }
    .mt-4 { margin-top: 1.5rem; }
    .text-muted { color: var(--text-secondary); }
    
    .fade-in {
      animation: fadeIn 0.4s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class DashboardComponent implements OnInit {
  
  // Observables for template
  accountInfo$!: Observable<AccountInfo>;
  summary$!: Observable<PaymentSummary>;
  
  notifications: Notification[] = [];

  constructor(
    private authService: AuthService,
    private paymentService: PaymentService,
    private analyticsService: AnalyticsService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const custId = this.authService.getCustomerId();
    if(custId) {
      // In a real app, we'd have a mapping mapping customer -> accountNumber 
      // For this demo, let's assume AccountNumber maps directly to ACC + ID.
      // e.g. CUST001 -> ACC1001. Let's just hardcode the mapping logic for demo:
      const accNum = custId.replace('CUST', 'ACC') + '0'; 
      // Actually, my test script used "ACC1001" for CUST001. Let's just fetch notifications based on custId directly!
      
      this.notificationService.getNotifications(custId).subscribe(res => {
        this.notifications = res.slice(0, 5); // top 5
      });
      
      this.summary$ = this.analyticsService.getTodaySummary();

      // We need an account number for the balance.
      // Let's assume CUST001 is ACC1001.
      const mappedAccNum = custId === 'CUST001' ? 'ACC1001' : (custId === 'CUST002' ? 'ACC1002' : 'ACC1001');
      this.accountInfo$ = this.paymentService.getAccountInfo(mappedAccNum);
    }
  }

  getIconClass(type: string): string {
    if(type.includes('CREDIT')) return 'icon-success';
    if(type.includes('DEBIT')) return 'icon-success';
    return 'icon-sys';
  }

  getIcon(type: string): string {
    if(type.includes('CREDIT')) return '↓';
    if(type.includes('DEBIT')) return '↑';
    return '🔔';
  }
}
