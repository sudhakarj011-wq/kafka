import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, RouterLink],
  template: `
    <div class="login-container">
      <div class="glass-card login-card">
        <div class="login-header">
          <div class="logo">⚡</div>
          <h2>Welcome to KafkaBank</h2>
          <p>Microservices & Event-Driven Architecture</p>
        </div>

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label">Customer ID</label>
            <input 
              type="text" 
              class="form-control" 
              formControlName="customerId" 
              placeholder="e.g., CUST001"
              autocomplete="off"
            >
            <div class="error" *ngIf="loginForm.get('customerId')?.touched && loginForm.get('customerId')?.invalid">
              Customer ID is required
            </div>
          </div>

          <button type="submit" class="btn-primary" [disabled]="loginForm.invalid || isLoading">
            {{ isLoading ? 'Authenticating...' : 'Access Account' }}
          </button>
        </form>
        
        <div class="demo-hints">
          <p>Demo Accounts:</p>
          <div class="badges">
            <span class="badge badge-success" (click)="fill('CUST001')">CUST001</span>
            <span class="badge badge-warning" (click)="fill('CUST002')">CUST002</span>
          </div>
          <p style="margin-top: 1rem">New here? <a routerLink="/register" class="register-link">Create an account</a></p>
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
      margin-bottom: 2.5rem;
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
    .demo-hints {
      margin-top: 2.5rem;
      text-align: center;
      border-top: 1px solid var(--glass-border);
      padding-top: 1.5rem;
    }
    .badges {
      display: flex;
      justify-content: center;
      gap: 1rem;
      margin-top: 0.8rem;
    }
    .badge {
      cursor: pointer;
      transition: transform 0.2s;
    }
    .badge:hover {
      transform: scale(1.1);
    }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      customerId: ['', Validators.required]
    });
  }

  fill(id: string) {
    this.loginForm.patchValue({ customerId: id });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      const { customerId } = this.loginForm.value;
      
      this.auth.login(customerId).subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          console.error('Login failed', err);
          alert('Login failed. Ensure API Gateway and Account Service are running.');
          this.isLoading = false;
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    }
  }
}
