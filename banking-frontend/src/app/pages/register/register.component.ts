import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, RouterLink],
  template: `
    <div class="login-container">
      <div class="glass-card login-card">
        <div class="login-header">
          <div class="logo">⚡</div>
          <h2>Create Account</h2>
          <p>Join KafkaBank — Event-Driven Banking</p>
        </div>

        <div class="success-msg" *ngIf="successMessage">
          ✅ {{ successMessage }}
        </div>
        <div class="error-msg" *ngIf="errorMessage">
          ❌ {{ errorMessage }}
        </div>

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label">Customer ID</label>
            <input
              type="text"
              class="form-control"
              formControlName="customerId"
              placeholder="e.g., CUST003"
              autocomplete="off"
            >
            <div class="error" *ngIf="registerForm.get('customerId')?.touched && registerForm.get('customerId')?.invalid">
              Customer ID is required
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Full Name</label>
            <input
              type="text"
              class="form-control"
              formControlName="customerName"
              placeholder="e.g., Ravi Kumar"
            >
            <div class="error" *ngIf="registerForm.get('customerName')?.touched && registerForm.get('customerName')?.invalid">
              Full name is required
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              type="email"
              class="form-control"
              formControlName="email"
              placeholder="e.g., ravi@example.com"
            >
            <div class="error" *ngIf="registerForm.get('email')?.touched && registerForm.get('email')?.invalid">
              Valid email is required
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Initial Deposit (₹)</label>
            <input
              type="number"
              class="form-control"
              formControlName="initialBalance"
              placeholder="e.g., 10000"
              min="0"
            >
            <div class="error" *ngIf="registerForm.get('initialBalance')?.touched && registerForm.get('initialBalance')?.invalid">
              Initial balance is required (min ₹0)
            </div>
          </div>

          <button type="submit" class="btn-primary" [disabled]="registerForm.invalid || isLoading">
            {{ isLoading ? 'Creating Account...' : 'Create Account' }}
          </button>
        </form>

        <div class="demo-hints">
          <p>Already have an account? <a routerLink="/login" class="link">Login here</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: grid;
      place-items: center;
      min-height: 100vh;
      padding: 1rem;
    }
    .login-card {
      width: 100%;
      max-width: 420px;
      padding: 3rem 2rem;
    }
    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .logo {
      background: var(--accent-gradient);
      width: 60px;
      height: 60px;
      border-radius: 16px;
      display: grid;
      place-items: center;
      font-size: 2rem;
      margin: 0 auto 1rem;
      box-shadow: 0 8px 25px rgba(99, 102, 241, 0.4);
    }
    h2 {
      font-size: 1.8rem;
      margin-bottom: 0.5rem;
    }
    p {
      color: var(--text-secondary);
      font-size: 0.95rem;
    }
    .error {
      color: var(--danger);
      font-size: 0.85rem;
      margin-top: 0.5rem;
    }
    .success-msg {
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: #10b981;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.9rem;
    }
    .error-msg {
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: var(--danger);
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.9rem;
    }
    .demo-hints {
      margin-top: 2rem;
      text-align: center;
      border-top: 1px solid var(--glass-border);
      padding-top: 1.5rem;
    }
    .link {
      color: var(--primary);
      text-decoration: none;
      font-weight: 500;
    }
    .link:hover {
      text-decoration: underline;
    }
  `]
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      customerId: ['', Validators.required],
      customerName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      initialBalance: [0, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.isLoading = true;
      this.successMessage = '';
      this.errorMessage = '';

      this.auth.register(this.registerForm.value).subscribe({
        next: () => {
          this.successMessage = 'Account created! Redirecting to login...';
          setTimeout(() => this.router.navigate(['/login']), 1500);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
          this.isLoading = false;
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    }
  }
}
