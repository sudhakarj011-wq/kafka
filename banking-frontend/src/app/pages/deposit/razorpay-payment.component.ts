import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { RazorpayService } from '../../services/razorpay.service';
import { AuthService } from '../../services/auth.service';

// Declare Razorpay as global (loaded from checkout.js CDN in index.html)
declare var Razorpay: any;

@Component({
  selector: 'app-razorpay-payment',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  template: `
    <div class="container fade-in">
      <div class="header">
        <h1>Deposit Money 💳</h1>
        <p class="subtitle">Add funds to your account instantly via Razorpay</p>
      </div>

      <div class="glass-card deposit-card">

        <!-- Amount Form -->
        <form [formGroup]="depositForm" (ngSubmit)="onPay()" *ngIf="!paymentSuccess">

          <div class="form-group">
            <label class="form-label">Your Account</label>
            <input type="text" class="form-control hint" formControlName="accountNumber" readonly>
            <small class="helper-text">Funds will be credited to this account.</small>
          </div>

          <div class="form-group">
            <label class="form-label">Amount (₹)</label>
            <input type="number" class="form-control amount-input"
                   formControlName="amount" placeholder="0.00" min="1">
            <small class="helper-text">Min: ₹1 · Max: ₹5,00,000</small>
          </div>

          <div class="payment-methods">
            <span class="method-chip">💳 Card</span>
            <span class="method-chip">📱 UPI</span>
            <span class="method-chip">🏦 NetBanking</span>
            <span class="method-chip">👛 Wallet</span>
          </div>

          <button type="submit" class="btn-pay" [disabled]="depositForm.invalid || isLoading">
            <span *ngIf="isLoading" class="spinner"></span>
            {{ isLoading ? 'Preparing Checkout...' : 'Proceed to Pay ₹' + (depositForm.get("amount")?.value || '0') }}
          </button>

          <div *ngIf="errorMessage" class="alert alert-danger mt-3">
            {{ errorMessage }}
          </div>
        </form>

        <!-- Success State -->
        <div *ngIf="paymentSuccess" class="success-state">
          <div class="success-icon">✅</div>
          <h2>Deposit Successful!</h2>
          <p class="success-amount">₹{{ successAmount }} added to your account</p>
          <p class="success-txn">Transaction ID: <strong>{{ successTxnId }}</strong></p>
          <p class="success-note">
            Your balance will be updated shortly. Kafka event has been published to notify all services.
          </p>
          <button class="btn-primary mt-3" (click)="resetForm()">Make Another Deposit</button>
        </div>
      </div>

      <!-- Info Card -->
      <div class="glass-card info-card" *ngIf="!paymentSuccess">
        <h3>🔒 Secure Payments</h3>
        <ul>
          <li>Powered by Razorpay — PCI DSS Level 1 certified</li>
          <li>256-bit SSL encryption</li>
          <li>Funds reflected within seconds via Kafka event pipeline</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .header { margin-bottom: 2rem; }
    .subtitle { color: var(--text-secondary); font-size: 1.1rem; margin-top: 0.5rem; }

    .deposit-card { max-width: 520px; }

    .amount-input {
      font-size: 1.8rem;
      font-weight: 700;
      color: var(--accent-primary) !important;
    }
    .amount-input::placeholder {
      font-weight: 400;
      color: rgba(255,255,255,0.2) !important;
    }
    .hint {
      color: var(--text-secondary) !important;
      background: rgba(0,0,0,0.2) !important;
    }
    .helper-text {
      display: block;
      margin-top: 0.4rem;
      color: var(--text-secondary);
      font-size: 0.8rem;
    }

    .payment-methods {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin: 1rem 0 1.5rem;
    }
    .method-chip {
      background: rgba(99,102,241,0.1);
      border: 1px solid rgba(99,102,241,0.3);
      padding: 0.4rem 0.8rem;
      border-radius: 999px;
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    .btn-pay {
      width: 100%;
      padding: 1rem 2rem;
      background: linear-gradient(135deg, #00b09b, #96c93d);
      color: white;
      border: none;
      border-radius: 14px;
      font-size: 1rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
    }
    .btn-pay:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 176, 155, 0.35);
    }
    .btn-pay:disabled { opacity: 0.6; cursor: not-allowed; }

    .spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      display: inline-block;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { 100% { transform: rotate(360deg); } }

    .alert { padding: 1rem; border-radius: 12px; font-weight: 500; }
    .alert-danger {
      background: var(--danger-bg);
      color: var(--danger);
      border: 1px solid rgba(239,68,68,0.2);
    }
    .mt-3 { margin-top: 1.5rem; }

    /* ─── Success State ─── */
    .success-state {
      text-align: center;
      padding: 2rem 1rem;
      animation: fadeIn 0.4s ease;
    }
    .success-icon { font-size: 4rem; margin-bottom: 1rem; }
    .success-amount {
      font-size: 2rem;
      font-weight: 700;
      background: linear-gradient(135deg, #00b09b, #96c93d);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin: 0.5rem 0;
    }
    .success-txn { color: var(--text-secondary); font-size: 0.9rem; }
    .success-note { color: var(--text-secondary); font-size: 0.85rem; margin-top: 1rem; opacity: 0.7; }

    /* ─── Info Card ─── */
    .info-card { max-width: 520px; margin-top: 1.5rem; padding: 1.5rem; }
    .info-card h3 { margin-bottom: 1rem; font-size: 1rem; }
    .info-card ul { padding-left: 1.2rem; color: var(--text-secondary); font-size: 0.85rem; }
    .info-card li { margin-bottom: 0.4rem; }

    .fade-in { animation: fadeIn 0.4s ease-out; }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class RazorpayPaymentComponent {
  depositForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  paymentSuccess = false;
  successAmount = 0;
  successTxnId = '';

  constructor(
    private fb: FormBuilder,
    private razorpayService: RazorpayService,
    private auth: AuthService
  ) {
    const accountNumber = this.auth.getAccountNumber() || '';

    this.depositForm = this.fb.group({
      accountNumber: [accountNumber, Validators.required],
      amount: ['', [Validators.required, Validators.min(1), Validators.max(500000)]]
    });
  }

  onPay() {
    if (this.depositForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const { amount, accountNumber } = this.depositForm.value;

    // Step 1: Create Razorpay order from backend
    this.razorpayService.createOrder({ amount, accountNumber, currency: 'INR' }).subscribe({
      next: (orderResp) => {
        this.isLoading = false;
        this.openRazorpayCheckout(orderResp, accountNumber, amount);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not initiate payment. Try again.';
      }
    });
  }

  private openRazorpayCheckout(orderResp: any, accountNumber: string, amount: number) {
    // Step 2: Open Razorpay native popup using the order details from backend
    const options = {
      key: orderResp.keyId,
      amount: Math.round(amount * 100), // paise
      currency: orderResp.currency,
      name: 'KafkaBank',
      description: `Deposit to ${accountNumber}`,
      order_id: orderResp.orderId,
      theme: { color: '#6366F1' },
      prefill: {
        name: 'Account Holder',
        email: 'user@kafkabank.com'
      },
      handler: (response: any) => {
        // Payment successful — Razorpay calls our webhook automatically
        // We just show success UI here
        this.paymentSuccess = true;
        this.successAmount = amount;
        this.successTxnId = response.razorpay_payment_id;
      },
      modal: {
        ondismiss: () => {
          this.errorMessage = 'Payment cancelled. Please try again.';
        }
      }
    };

    const rzp = new Razorpay(options);
    rzp.open();
  }

  resetForm() {
    this.paymentSuccess = false;
    this.errorMessage = '';
    this.depositForm.patchValue({ amount: '' });
  }
}
