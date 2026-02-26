import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslationService } from '../../services/translation.service';

@Component({
  selector: 'app-bottom-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="bottom-nav">
      <a routerLink="/"
         routerLinkActive="active"
         [routerLinkActiveOptions]="{ exact: true }"
         class="bottom-nav-item">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <span>{{ t('header.home') }}</span>
      </a>

      <a routerLink="/search"
         routerLinkActive="active"
         [routerLinkActiveOptions]="{ exact: false }"
         class="bottom-nav-item">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
        <span>{{ t('header.explore') }}</span>
      </a>

      <a routerLink="/search"
         [queryParams]="{ tab: 'scene' }"
         routerLinkActive="active"
         [routerLinkActiveOptions]="{ exact: false }"
         class="bottom-nav-item">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
        <span>{{ t('header.advanced') }}</span>
      </a>
    </nav>
  `,
  styleUrl: './bottom-nav.component.scss'
})
export class BottomNavComponent {
  private ts = inject(TranslationService);

  t(key: string): string { return this.ts.t(key); }
}
