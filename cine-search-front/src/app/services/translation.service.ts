import { Injectable, signal, computed } from '@angular/core';
import { FR } from '../i18n/fr';
import { EN } from '../i18n/en';

export type Lang = 'fr' | 'en';

const TRANSLATIONS: Record<Lang, Record<string, string>> = { fr: FR, en: EN };
const STORAGE_KEY = 'movieseeker-lang';

/** Lightweight Signal-based translation service. */
@Injectable({ providedIn: 'root' })
export class TranslationService {

  /** Current active language. */
  readonly lang = signal<Lang>(this.loadLang());

  /** TMDB-compatible locale string (e.g. "fr-FR", "en-US"). */
  readonly locale = computed(() => this.lang() === 'fr' ? 'fr-FR' : 'en-US');

  /** Returns the translated string for the given key. */
  t(key: string): string {
    return TRANSLATIONS[this.lang()][key] ?? key;
  }

  /** Switches language and persists to localStorage. */
  setLang(lang: Lang): void {
    this.lang.set(lang);
    try { localStorage.setItem(STORAGE_KEY, lang); } catch { /* ignored */ }
  }

  /** Reads persisted language from localStorage. */
  private loadLang(): Lang {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'en' || stored === 'fr') return stored;
    } catch { /* ignored */ }
    return 'fr';
  }
}
