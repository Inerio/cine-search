import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header.component';
import { BottomNavComponent } from './components/bottom-nav/bottom-nav.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, BottomNavComponent],
  template: `
    <app-header />
    <main class="main-content">
      <router-outlet />
    </main>
    <app-bottom-nav />
  `,
  styles: [`
    .main-content {
      min-height: calc(100vh - var(--header-height, 70px));
      padding-top: var(--header-height, 70px);
    }

    @media (max-width: 768px) {
      .main-content {
        min-height: calc(100vh - var(--header-height-mobile, 56px));
        padding-top: var(--header-height-mobile, 56px);
        padding-bottom: var(--bottom-nav-height, 56px);
      }
    }
  `]
})
export class AppComponent {}
