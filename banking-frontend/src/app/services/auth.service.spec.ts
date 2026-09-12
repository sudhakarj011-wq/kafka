import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

/**
 * ============================================================
 * AuthService Tests
 * ============================================================
 * INTERVIEW TIP — Why test localStorage in service unit tests?
 * AuthService uses localStorage to cache JWT token, customerId
 * and accountNumber. If these methods break, the entire
 * authenticated flow breaks (Dashboard, Transfer, History).
 * Testing them verifies the critical state management layer.
 *
 * INTERVIEW TIP — What is spyOn()?
 * spyOn() monitors a method call on an existing object without
 * replacing it entirely (unlike SpyObj which creates a fake).
 * Here we spy on localStorage to verify setItem() was called
 * with the correct keys and values.
 * ============================================================
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clean localStorage before each test for isolation
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * TEST: login()
   * Verifies: POST /api/auth/login → then GET /api/accounts/by-customer/{id}
   *
   * INTERVIEW TIP — login() uses switchMap (RxJS chaining):
   * 1. POST /api/auth/login  → stores JWT in localStorage
   * 2. GET  /api/accounts/by-customer/{id} → stores accountNumber
   * Both requests happen in sequence. We need to flush both.
   */
  it('should login, store token in localStorage and emit isLoggedIn$ as true', () => {
    const mockTokenResponse = { token: 'jwt-mock-token-abc123' };
    const mockAccountResponse = { accountNumber: 'ACC1001' };

    spyOn(localStorage, 'setItem').and.callThrough();

    // ACT
    service.login('CUST001').subscribe(response => {
      expect(response.token).toBe('jwt-mock-token-abc123');
    });

    // ASSERT — 1st Request: POST login
    const loginReq = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(loginReq.request.method).toBe('POST');
    expect(loginReq.request.body).toEqual({ customerId: 'CUST001' });
    loginReq.flush(mockTokenResponse);

    // ASSERT — 2nd Request: GET account (auto-chained via switchMap)
    const accountReq = httpMock.expectOne(`${environment.apiUrl}/accounts/by-customer/CUST001`);
    expect(accountReq.request.method).toBe('GET');
    accountReq.flush(mockAccountResponse);

    // ASSERT — localStorage should have token and IDs
    expect(localStorage.setItem).toHaveBeenCalledWith('auth_token', 'jwt-mock-token-abc123');
    expect(localStorage.setItem).toHaveBeenCalledWith('customer_id', 'CUST001');
    expect(localStorage.setItem).toHaveBeenCalledWith('account_number', 'ACC1001');
  });

  /**
   * TEST: isLoggedIn$ observable emits correctly
   * Verifies BehaviorSubject state after login
   */
  it('should emit true on isLoggedIn$ after successful login', () => {
    let emittedValue: boolean | undefined;

    service.isLoggedIn$.subscribe(val => (emittedValue = val));

    service.login('CUST001').subscribe();

    // Flush both chained requests
    httpMock.expectOne(`${environment.apiUrl}/auth/login`)
      .flush({ token: 'mock-token' });
    httpMock.expectOne(`${environment.apiUrl}/accounts/by-customer/CUST001`)
      .flush({ accountNumber: 'ACC1001' });

    expect(emittedValue).toBeTrue();
  });

  /**
   * TEST: logout()
   * Verifies: localStorage cleared + isLoggedIn$ emits false
   */
  it('should clear localStorage and emit false on isLoggedIn$ after logout', () => {
    // Pre-set some mock localStorage values
    localStorage.setItem('auth_token', 'some-token');
    localStorage.setItem('customer_id', 'CUST001');
    localStorage.setItem('account_number', 'ACC1001');

    let emittedValue: boolean | undefined;
    service.isLoggedIn$.subscribe(val => (emittedValue = val));

    // ACT
    service.logout();

    // ASSERT
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('customer_id')).toBeNull();
    expect(localStorage.getItem('account_number')).toBeNull();
    expect(emittedValue).toBeFalse();
  });

  /**
   * TEST: getToken()
   * Verifies it reads from localStorage correctly
   */
  it('should return token from localStorage via getToken()', () => {
    expect(service.getToken()).toBeNull(); // Initially null

    localStorage.setItem('auth_token', 'test-token-xyz');
    expect(service.getToken()).toBe('test-token-xyz');
  });

  /**
   * TEST: getCustomerId()
   */
  it('should return customer ID from localStorage', () => {
    expect(service.getCustomerId()).toBeNull();

    localStorage.setItem('customer_id', 'CUST002');
    expect(service.getCustomerId()).toBe('CUST002');
  });

  /**
   * TEST: getAccountNumber()
   */
  it('should return account number from localStorage', () => {
    expect(service.getAccountNumber()).toBeNull();

    localStorage.setItem('account_number', 'ACC1002');
    expect(service.getAccountNumber()).toBe('ACC1002');
  });
});
