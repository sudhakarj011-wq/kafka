import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router';

// Simple Route Guard
const authGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.getToken()) {
    return true;
  }
  return router.parseUrl('/login');
};

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', loadComponent: () => import('./pages/register/register.component').then(c => c.RegisterComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(c => c.DashboardComponent) },
      { path: 'transfer', loadComponent: () => import('./pages/transfer/transfer.component').then(c => c.TransferComponent) },
      { path: 'history', loadComponent: () => import('./pages/history/history.component').then(c => c.HistoryComponent) },
      { path: 'deposit', loadComponent: () => import('./pages/deposit/razorpay-payment.component').then(c => c.RazorpayPaymentComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
