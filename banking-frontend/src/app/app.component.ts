import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { AsyncPipe, NgIf } from '@angular/common';
import { SidebarComponent } from './components/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, AsyncPipe, NgIf, SidebarComponent],
  template: `
    <div class="app-layout">
      <!-- Show sidebar only if logged in -->
      <app-sidebar *ngIf="auth.isLoggedIn$ | async"></app-sidebar>
      
      <main class="main-content" [class.full-width]="!(auth.isLoggedIn$ | async)">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      min-height: 100vh;
    }
    .main-content {
      flex: 1;
      padding: 0;
      overflow-y: auto;
      height: 100vh;
      margin-left: 280px; /* Sidebar width */
      transition: margin 0.3s ease;
    }
    .main-content.full-width {
      margin-left: 0;
    }
  `]
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
