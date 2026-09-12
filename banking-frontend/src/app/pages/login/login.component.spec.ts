import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login.component';

/**
 * ============================================================
 * INTERVIEW TIPS FOR ANGULAR TESTING
 * ============================================================
 * Q: Why do we use 'jasmine.createSpyObj' instead of real services?
 * A: True Unit Testing requires ISOLATION. By using spies (mocks),
 *    we test the Component's logic, not the Router or HTTP logic.
 *    If AuthService breaks, this test still passes because it's
 *    testing the Component, not the Service.
 *
 * Q: What does TestBed do?
 * A: It creates a dynamic Angular testing module that emulates
 *    an @NgModule. This is where we configure dependencies.
 *
 * Q: Why do we test standalone components differently in imports?
 * A: For standalone components, we add them to 'imports', not
 *    'declarations' because they manage their own dependencies.
 * ============================================================
 */
describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  // Define our mock services
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    // Create actual spy objects with the methods we plan to use
    mockAuthService = jasmine.createSpyObj('AuthService', ['login']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    // Configure the testing environment
    await TestBed.configureTestingModule({
      // For standalone components, the component goes in imports
      imports: [LoginComponent, ReactiveFormsModule],
      // Provide the mocked services in place of the real ones
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    // Trigger initial data binding (ngOnInit)
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should have an invalid form initially when fields are empty', () => {
    expect(component.loginForm.valid).toBeFalse();
  });

  it('should fill the form using the fill() method', () => {
    component.fill('CUST001');
    expect(component.loginForm.get('customerId')?.value).toBe('CUST001');
    expect(component.loginForm.valid).toBeTrue();
  });

  it('should call AuthService login and navigate on successful submission', () => {
    // ARRANGE: Setup the mock to return an Observable that emits successfully
    mockAuthService.login.and.returnValue(of({} as any));
    
    // ACT: Fill the form and submit
    component.fill('VALID_CUSTOMER');
    component.onSubmit();

    // ASSERT: Verify the interactions
    expect(mockAuthService.login).toHaveBeenCalledWith('VALID_CUSTOMER');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.isLoading).toBeFalse();
  });

  it('should handle login error gracefully without navigating', () => {
    // ARRANGE: Setup the mock to simulate an HTTP error
    mockAuthService.login.and.returnValue(throwError(() => ({ status: 401 })));
    spyOn(console, 'error'); // Prevent console.error from polluting test output
    spyOn(window, 'alert'); // We know the component calls alert

    // ACT: Fill the form and submit
    component.fill('INVALID_CUSTOMER');
    component.onSubmit();

    // ASSERT
    expect(mockAuthService.login).toHaveBeenCalledWith('INVALID_CUSTOMER');
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(window.alert).toHaveBeenCalled();
    expect(component.isLoading).toBeFalse();
  });
});
