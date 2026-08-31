import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar glass-card">
      <div class="brand">
        <div class="logo">⚡</div>
        <h2>KafkaBank</h2>
      </div>
      
      <nav class="nav-menu">
        <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
          <span>📊</span> Dashboard
        </a>
        <a routerLink="/transfer" routerLinkActive="active" class="nav-item">
          <span>💸</span> Transfer
        </a>
        <a routerLink="/history" routerLinkActive="active" class="nav-item">
          <span>📜</span> History
        </a>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-logout" (click)="logout()">
          🚪 Logout
        </button>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      width: 280px;
      height: 100vh;
      position: fixed;
      left: 0;
      top: 0;
      display: flex;
      flex-direction: column;
      border-radius: 0;
      border-left: none;
      border-top: none;
      border-bottom: none;
      z-index: 100;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 0 2rem 0;
      border-bottom: 1px solid rgba(255,255,255,0.05);
      margin-bottom: 2rem;
    }
    .logo {
      background: var(--accent-gradient);
      width: 40px;
      height: 40px;
      border-radius: 12px;
      display: grid;
      place-items: center;
      font-size: 1.2rem;
    }
    .nav-menu {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      flex: 1;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.5rem;
      color: var(--text-secondary);
      text-decoration: none;
      border-radius: 12px;
      transition: all 0.2s ease;
      font-weight: 500;
    }
    .nav-item:hover {
      background: rgba(255,255,255,0.05);
      color: white;
    }
    .nav-item.active {
      background: var(--accent-gradient);
      color: white;
      box-shadow: 0 4px 15px rgba(99, 102, 241, 0.2);
    }
    .btn-logout {
      width: 100%;
      padding: 1rem;
      background: rgba(239, 68, 68, 0.1);
      color: var(--danger);
      border: 1px solid rgba(239, 68, 68, 0.2);
      border-radius: 12px;
      cursor: pointer;
      font-weight: 600;
      transition: all 0.2s;
    }
    .btn-logout:hover {
      background: rgba(239, 68, 68, 0.2);
    }
  `]
})
export class SidebarComponent {
  constructor(private auth: AuthService, private router: Router) {}

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
