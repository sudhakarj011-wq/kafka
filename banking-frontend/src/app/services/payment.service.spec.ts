import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService, TransferRequest, PaymentResponse } from './payment.service';
import { environment } from '../../environments/environment';

/**
 * ============================================================
 * PaymentService Tests
 * ============================================================
 * INTERVIEW TIP — What is AAA Pattern?
 * Arrange-Act-Assert is the standard pattern for writing tests.
 *
 * INTERVIEW TIP — Why test services separately from components?
 * Services contain business logic. If we test them separately,
 * component tests can mock the service and focus only on UI
 * behavior — not HTTP plumbing. This is true layered testing.
 * ============================================================
 */
describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const apiBase = `${environment.apiUrl}/payments`;

  // Reusable mock transfer request
  const mockTransferRequest: TransferRequest = {
    fromAccount: 'ACC1001',
    toAccount: 'ACC1002',
    amount: 5000
  };

  // Reusable mock payment response
  const mockPaymentResponse: PaymentResponse = {
    transactionId: 'TXN-UUID-12345',
    status: 'SUCCESS',
    message: 'Transfer completed via Kafka Saga'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // No pending HTTP requests should remain
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * TEST: transferMoney()
   * Verifies: POST /api/payments/transfer
   *
   * INTERVIEW TIP — What happens on backend when this is called?
   * PaymentCommandService → AccountGrpcClient.debitAccount() [gRPC]
   *                       → Kafka Event published
   *                       → AccountSagaConsumer.creditAccount() [gRPC]
   * Angular only sees the REST response. gRPC is transparent to Angular.
   */
  it('should POST transfer request and return PaymentResponse', () => {
    // ACT
    service.transferMoney(mockTransferRequest).subscribe(response => {
      // ASSERT response
      expect(response.transactionId).toBe('TXN-UUID-12345');
      expect(response.status).toBe('SUCCESS');
    });

    // ASSERT HTTP request
    const req = httpMock.expectOne(`${apiBase}/transfer`);
    expect(req.request.method).toBe('POST');

    // Verify request body (all 3 fields should be sent as-is)
    expect(req.request.body.fromAccount).toBe('ACC1001');
    expect(req.request.body.toAccount).toBe('ACC1002');
    expect(req.request.body.amount).toBe(5000);

    // Inject mock response
    req.flush(mockPaymentResponse);
  });

  /**
   * TEST: getTransactionHistory()
   * Verifies: GET /api/payments/history/{accountNumber}
   */
  it('should GET transaction history for a given account number', () => {
    const mockHistory = [
      { transactionId: 'TXN-001', amount: 1000, status: 'SUCCESS', fromAccount: 'ACC1001', toAccount: 'ACC1002' },
      { transactionId: 'TXN-002', amount: 2000, status: 'FAILED', fromAccount: 'ACC1003', toAccount: 'ACC1001' }
    ];

    service.getTransactionHistory('ACC1001').subscribe(history => {
      expect(history.length).toBe(2);
      expect(history[0].status).toBe('SUCCESS');
      expect(history[1].status).toBe('FAILED');
    });

    const req = httpMock.expectOne(`${apiBase}/history/ACC1001`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHistory);
  });

  /**
   * TEST: Empty transaction history
   * Edge case — account has no transactions yet
   */
  it('should return empty array when account has no transaction history', () => {
    service.getTransactionHistory('ACC9999').subscribe(history => {
      expect(history.length).toBe(0);
    });

    const req = httpMock.expectOne(`${apiBase}/history/ACC9999`);
    req.flush([]); // Backend returns empty list
  });

  /**
   * TEST: Transfer failure
   * Verifies graceful handling of 400 bad request (e.g. insufficient funds)
   */
  it('should propagate HTTP 400 error on transfer failure', () => {
    service.transferMoney(mockTransferRequest).subscribe({
      error: (err) => {
        expect(err.status).toBe(400);
      }
    });

    const req = httpMock.expectOne(`${apiBase}/transfer`);
    req.flush(
      { message: 'Insufficient balance' },
      { status: 400, statusText: 'Bad Request' }
    );
  });
});
