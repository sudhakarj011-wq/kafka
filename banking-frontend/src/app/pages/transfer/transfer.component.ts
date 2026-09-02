import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService } from '../../services/payment.service';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  template: `
    <div class="container fade-in">
      <div class="header">
        <h1>Transfer Money</h1>
        <p class="subtitle">Send money instantly across the Kafka fabric 🚀</p>
      </div>

      <div class="glass-card transfer-card">
        <form [formGroup]="transferForm" (ngSubmit)="onSubmit()">
          
          <div class="form-group">
            <label class="form-label">From Account (Auto-mapped)</label>
            <input type="text" class="form-control hint" formControlName="fromAccount" readonly>
            <small class="helper-text">Mapped from your Customer ID.</small>
          </div>

          <div class="form-group">
            <label class="form-label">Recipient Account No</label>
            <input type="text" class="form-control" formControlName="toAccount" placeholder="e.g. ACC1002">
          </div>

          <div class="form-group">
            <label class="form-label">Amount (₹)</label>
            <input type="number" class="form-control amount-input" formControlName="amount" placeholder="0.00">
          </div>

          <button type="submit" class="btn-primary" [disabled]="transferForm.invalid || isProcessing">
            <span *ngIf="isProcessing" class="spinner"></span>
            {{ isProcessing ? 'Processing Transaction...' : 'Send Money Now' }}
          </button>
          
          <div *ngIf="successMessage" class="alert alert-success mt-3">
            {{ successMessage }}
            <p style="font-size: 0.85rem; margin-top: 0.5rem; opacity: 0.8">
              Kafka Outbox Pattern is now actively syncing this to other microservices!
            </p>
          </div>
          
          <div *ngIf="errorMessage" class="alert alert-danger mt-3">
            {{ errorMessage }}
          </div>
        </form>
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
    .transfer-card {
      max-width: 500px;
    }
    .amount-input {
      font-size: 1.5rem;
      font-weight: 600;
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
    .mt-3 { margin-top: 1.5rem; }
    
    .alert {
      padding: 1rem;
      border-radius: 12px;
      font-weight: 500;
      animation: fadeIn 0.3s ease;
    }
    .alert-success {
      background: var(--success-bg);
      color: var(--success);
      border: 1px solid rgba(16, 185, 129, 0.2);
    }
    .alert-danger {
      background: var(--danger-bg);
      color: var(--danger);
      border: 1px solid rgba(239, 68, 68, 0.2);
    }
    
    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      display: inline-block;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { 100% { transform: rotate(360deg); } }
    
    .fade-in {
      animation: fadeIn 0.4s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class TransferComponent {
  transferForm: FormGroup;
  isProcessing = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private auth: AuthService,
    private router: Router
  ) {
    // Use the accountNumber cached in localStorage by AuthService at login time
    // No hardcoded mapping needed — works dynamically for any customer
    const fromAcc = this.auth.getAccountNumber() || '';

    this.transferForm = this.fb.group({
      fromAccount: [fromAcc, Validators.required],
      toAccount: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(1)]]
    });
  }

  onSubmit() {
    if (this.transferForm.valid) {
      this.isProcessing = true;
      this.successMessage = '';
      this.errorMessage = '';

      this.paymentService.transferMoney(this.transferForm.value).subscribe({
        next: (res) => {
          this.successMessage = `Transfer initiated successfully! Txn ID: ${res.transactionId}`;
          this.isProcessing = false;
          this.transferForm.reset({ fromAccount: this.transferForm.get('fromAccount')?.value });
          
          // Re-navigate to dashboard after 2 seconds to see notifications
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 3000);
        },
        error: (err) => {
          // If the backend returns 400 Bad Request, handle it nicely
          this.errorMessage = err.error?.message || 'Transfer failed. Check balance limits or account info.';
          this.isProcessing = false;
        }
      });
    }
  }
}
