import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <header class="header">
      <a routerLink="/" class="logo">
        <span class="logo-icon">🎬</span>
        <span class="logo-text">Cine<span class="accent">Search</span></span>
      </a>

      <div class="search-bar">
        <input
          type="text"
          [ngModel]="query()"
          (ngModelChange)="query.set($event)"
          (keyup.enter)="onSearch()"
          placeholder="Rechercher un film, un acteur..."
          class="search-input"
        />
        <button class="search-btn" (click)="onSearch()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
        </button>
      </div>

      <nav class="nav-links">
        <a routerLink="/" class="nav-link">Accueil</a>
        <a routerLink="/search" [queryParams]="{ tab: 'scene' }" class="nav-link ai-link">
          ✨ Recherche IA
        </a>
      </nav>
    </header>
  `,
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private router = inject(Router);
  query = signal('');

  onSearch(): void {
    const q = this.query().trim();
    if (q) {
      this.router.navigate(['/search'], { queryParams: { q, tab: 'movie' } });
    }
  }
}
