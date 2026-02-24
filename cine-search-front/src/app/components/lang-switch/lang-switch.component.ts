import { Component, inject } from '@angular/core';
import { TranslationService, Lang } from '../../services/translation.service';

@Component({
  selector: 'app-lang-switch',
  standalone: true,
  template: `
    <div class="lang-toggle">
      <button
        class="lang-btn"
        [class.active]="ts.lang() === 'fr'"
        (click)="setLang('fr')">
        FR
      </button>
      <button
        class="lang-btn"
        [class.active]="ts.lang() === 'en'"
        (click)="setLang('en')">
        EN
      </button>
    </div>
  `,
  styles: [`
    .lang-toggle {
      display: flex;
      gap: 2px;
      background: rgba(255, 255, 255, 0.06);
      border-radius: 6px;
      padding: 2px;
    }
    .lang-btn {
      padding: 4px 10px;
      font-size: 0.75rem;
      font-weight: 600;
      font-family: inherit;
      letter-spacing: 0.5px;
      color: rgba(255, 255, 255, 0.4);
      background: transparent;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: rgba(255, 255, 255, 0.7);
      }

      &.active {
        color: #fff;
        background: #a51d1d;
      }
    }
  `]
})
export class LangSwitchComponent {
  ts = inject(TranslationService);

  setLang(lang: Lang): void {
    this.ts.setLang(lang);
  }
}
