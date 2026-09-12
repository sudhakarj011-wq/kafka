import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountService, AccountInfo, BalanceUpdateRequest } from './account.service';
import { environment } from '../../environments/environment';

/**
 * ============================================================
 * AccountService Tests
 * ============================================================
 * INTERVIEW TIP — Why HttpClientTestingModule?
 * - Real HttpClient would make actual HTTP calls to the backend.
 * - HttpClientTestingModule intercepts them so we can control
 *   the response manually (mocked data) — true unit isolation.
 *
 * PATTERN USED — Arrange → Act → Assert (AAA)
 * - Arrange: Setup mocks and expected data
 * - Act:     Call the method under test
 * - Assert:  Verify HTTP request and response
 * ============================================================
 */
describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;

  const apiBase = `${environment.apiUrl}/accounts`;

  // Reusable mock data — shared across tests to avoid duplication
  const mockAccount: AccountInfo = {
    id: 1,
    accountNumber: 'ACC1001',
    customerId: 'CUST001',
    customerName: 'Sudhakar',
    balance: 50000,
    status: 'ACTIVE'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  // Ensures no pending HTTP requests remain after each test
  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * TEST: getAccount()
   * Verifies: GET /api/accounts/{accountNumber}
   * Maps to backend gRPC: AccountService.getAccount()
   */
  it('should GET account details by account number', () => {
    // ACT
    service.getAccount('ACC1001').subscribe(account => {
      // ASSERT response
      expect(account.accountNumber).toBe('ACC1001');
      expect(account.customerName).toBe('Sudhakar');
      expect(account.balance).toBe(50000);
    });

    // ASSERT HTTP request
    const req = httpMock.expectOne(`${apiBase}/ACC1001`);
    expect(req.request.method).toBe('GET');

    // Inject mock response
    req.flush(mockAccount);
  });

  /**
   * TEST: getAccountByCustomerId()
   * Verifies: GET /api/accounts/by-customer/{customerId}
   * Used by AuthService at login to resolve customerId → accountNumber
   */
  it('should GET account details by customer ID', () => {
    service.getAccountByCustomerId('CUST001').subscribe(account => {
      expect(account.customerId).toBe('CUST001');
      expect(account.accountNumber).toBe('ACC1001');
    });

    const req = httpMock.expectOne(`${apiBase}/by-customer/CUST001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAccount);
  });

  /**
   * TEST: debitAccount()
   * Verifies: PUT /api/accounts/{accountNumber}/debit
   * Maps to backend gRPC: AccountService.debitAccount()
   */
  it('should PUT debit request and return updated account', () => {
    const debitRequest: BalanceUpdateRequest = { amount: 1000 };
    const updatedAccount: AccountInfo = { ...mockAccount, balance: 49000 };

    service.debitAccount('ACC1001', debitRequest).subscribe(account => {
      expect(account.balance).toBe(49000); // Balance reduced after debit
    });

    const req = httpMock.expectOne(`${apiBase}/ACC1001/debit`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(debitRequest); // Request body check
    req.flush(updatedAccount);
  });

  /**
   * TEST: creditAccount()
   * Verifies: PUT /api/accounts/{accountNumber}/credit
   * Maps to backend gRPC: AccountService.creditAccount()
   */
  it('should PUT credit request and return updated account', () => {
    const creditRequest: BalanceUpdateRequest = { amount: 5000 };
    const updatedAccount: AccountInfo = { ...mockAccount, balance: 55000 };

    service.creditAccount('ACC1001', creditRequest).subscribe(account => {
      expect(account.balance).toBe(55000); // Balance increased after credit
    });

    const req = httpMock.expectOne(`${apiBase}/ACC1001/credit`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(creditRequest);
    req.flush(updatedAccount);
  });

  /**
   * TEST: Error Handling
   * Verifies that HTTP 404 errors are passed through correctly
   */
  it('should propagate HTTP 404 error when account not found', () => {
    service.getAccount('INVALID_ACC').subscribe({
      error: (err) => {
        expect(err.status).toBe(404);
      }
    });

    const req = httpMock.expectOne(`${apiBase}/INVALID_ACC`);
    // Simulate backend 404 error
    req.flush('Account not found', { status: 404, statusText: 'Not Found' });
  });
});
