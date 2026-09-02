import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  token: string;
}

export interface RegisterRequest {
  customerId: string;
  customerName: string;
  email: string;
  initialBalance: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // Reactive state for UI updates
  private isLoggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  private currentCustomerSubject = new BehaviorSubject<string | null>(this.getCustomerId());
  public currentCustomer$ = this.currentCustomerSubject.asObservable();

  constructor(private http: HttpClient) { }

  /**
   * Login — POST /api/auth/login
   * After getting the JWT, fetches the accountNumber for the customerId
   * and stores it in localStorage so Dashboard can display balance without hardcoded mapping.
   */
  login(customerId: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { customerId }).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('auth_token', response.token);
          localStorage.setItem('customer_id', customerId);
          this.isLoggedInSubject.next(true);
          this.currentCustomerSubject.next(customerId);
        }
      }),
      // After login, resolve customerId → accountNumber and cache it
      switchMap(response => {
        return this.http.get<any>(`${environment.apiUrl}/accounts/by-customer/${customerId}`).pipe(
          tap(account => {
            if (account?.accountNumber) {
              localStorage.setItem('account_number', account.accountNumber);
            }
          }),
          // Return original auth response so callers still get the token
          switchMap(() => [response])
        );
      })
    );
  }

  /**
   * Register — POST /api/accounts/register
   * Public route in gateway — no JWT needed.
   */
  register(request: RegisterRequest): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}/accounts/register`, {
      customerId: request.customerId,
      customerName: request.customerName,
      initialBalance: request.initialBalance
    });
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('customer_id');
    localStorage.removeItem('account_number');
    this.isLoggedInSubject.next(false);
    this.currentCustomerSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  private hasToken(): boolean {
    return !!this.getToken();
  }

  getCustomerId(): string | null {
    return localStorage.getItem('customer_id');
  }

  /** Returns the cached account number resolved at login time */
  getAccountNumber(): string | null {
    return localStorage.getItem('account_number');
  }
}

