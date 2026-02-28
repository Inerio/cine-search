import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { TranslationService } from '../../services/translation.service';
import { Movie, AiMovieQuery } from '../../models/movie.model';

@Component({
  selector: 'app-scene-search',
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="scene-search">
      <div class="advanced-banner">
        <h2>{{ t('scene.title') }}</h2>
        <p>{{ t('scene.subtitle') }}</p>
      </div>

      <div class="prompt-area">
        <div class="media-type-selector">
          <button class="media-type-btn" [class.active]="mediaType() === 'all'" (click)="mediaType.set('all')">{{ t('scene.mediaAll') }}</button>
          <button class="media-type-btn" [class.active]="mediaType() === 'movie'" (click)="mediaType.set('movie')">{{ t('scene.mediaMovie') }}</button>
          <button class="media-type-btn" [class.active]="mediaType() === 'tv'" (click)="mediaType.set('tv')">{{ t('scene.mediaTv') }}</button>
        </div>
        <textarea
          [ngModel]="description()"
          (ngModelChange)="description.set($event)"
          (keydown)="onKeydown($event)"
          [placeholder]="t('scene.placeholder')"
          class="textarea"
          rows="4"
        ></textarea>
        <div class="prompt-footer">
          <span class="hint">{{ t('scene.hint') }}</span>
          <button
            class="btn-search"
            (click)="search()"
            [disabled]="loading() || description().trim().length < 2">
            @if (loading()) {
              <span class="spinner-small"></span>
              {{ t('scene.loading') }}
            } @else {
              {{ t('scene.search') }}
            }
          </button>
        </div>
      </div>

      @if (loading()) {
        <div class="search-loading">
          <div class="pulse-ring"></div>
          <p>{{ t('scene.loading') }}</p>
        </div>
      }

      @if (parsedQuery() && !loading()) {
        <div class="parsed-results">
          <span class="parsed-badge">{{ intentLabel(parsedQuery()!.intent) }}</span>
          @if (parsedQuery()!.type) {
            <span class="parsed-tag type-tag">{{ parsedQuery()!.type === 'tv' ? t('scene.typeTV') : t('scene.typeMovie') }}</span>
          }
          @if (parsedQuery()!.confidence) {
            <span class="confidence-indicator" [class]="'confidence-' + parsedQuery()!.confidence">
              {{ confidenceLabel(parsedQuery()!.confidence!) }}
            </span>
          }
          @if (parsedQuery()!.genres && parsedQuery()!.genres!.length > 0) {
            @for (genre of parsedQuery()!.genres!; track genre) {
              <span class="parsed-tag">{{ genre }}</span>
            }
          }
          @if (parsedQuery()!.actors && parsedQuery()!.actors!.length > 0) {
            @for (actor of parsedQuery()!.actors!; track actor) {
              <span class="parsed-tag actor-tag">🎭 {{ actor }}</span>
            }
          }
          @if (parsedQuery()!.year) {
            <span class="parsed-tag">{{ parsedQuery()!.year }}</span>
          }
        </div>
        @if (parsedQuery()!.explanation) {
          <p class="ai-explanation">{{ parsedQuery()!.explanation }}</p>
        }
      }

      <!-- Best Match -->
      @if (bestMatch() && !loading()) {
        <div class="best-match-section">
          <h3>{{ t('scene.bestMatch') }}</h3>
          <div class="best-match-card">
            <app-movie-card [movie]="bestMatch()!" [showMediaBadge]="true" />
          </div>
        </div>
      }

      <!-- Similar Movies -->
      @if (similarMovies().length > 0 && !loading()) {
        <div class="similar-section">
          <h3>{{ t('scene.similarMovies') }}</h3>
          <div class="movie-grid">
            @for (movie of similarMovies(); track movie.id) {
              <app-movie-card [movie]="movie" [showMediaBadge]="true" />
            }
          </div>
        </div>
      }

      <!-- Other Results -->
      @if (results().length > 0 && !loading()) {
        <div class="results-header">
          <h3>{{ bestMatch() ? t('scene.otherResults') : results().length + ' ' + t('scene.resultsCount') }}</h3>
        </div>
        <div class="movie-grid">
          @for (movie of results(); track movie.id) {
            <app-movie-card [movie]="movie" [showMediaBadge]="true" />
          }
        </div>
      }

      @if (!loading() && searched() && !bestMatch() && results().length === 0) {
        <div class="empty-state">
          @if (parsedQuery()?.intent === 'unknown') {
            <p>{{ t('scene.notRelated') }}</p>
          } @else {
            <p>{{ t('scene.noMatch') }}<br>
            {{ t('scene.tryAgain') }}</p>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './scene-search.component.scss'
})
export class SceneSearchComponent {
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);

  description = signal('');
  mediaType = signal<string>('all');
  results = signal<Movie[]>([]);
  bestMatch = signal<Movie | null>(null);
  similarMovies = signal<Movie[]>([]);
  parsedQuery = signal<AiMovieQuery | null>(null);
  loading = signal(false);
  searched = signal(false);

  t(key: string): string { return this.ts.t(key); }

  /** Ctrl+Enter triggers search from textarea. */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && event.ctrlKey) {
      this.search();
    }
  }

  /** Sends the free-text description to the AI parse endpoint. */
  search(): void {
    const desc = this.description().trim();
    if (desc.length < 2 || this.loading()) return;

    this.loading.set(true);
    this.searched.set(true);
    this.parsedQuery.set(null);
    this.bestMatch.set(null);
    this.similarMovies.set([]);

    this.movieService.aiParse(desc, this.mediaType()).subscribe({
      next: response => {
        this.parsedQuery.set(response.parsed);
        this.bestMatch.set(response.bestMatch);
        this.similarMovies.set(response.similarMovies ?? []);
        this.results.set(response.results);
        this.loading.set(false);
      },
      error: () => {
        this.results.set([]);
        this.bestMatch.set(null);
        this.similarMovies.set([]);
        this.parsedQuery.set(null);
        this.loading.set(false);
      }
    });
  }

  /** Maps intent codes to user-friendly labels. */
  intentLabel(intent: string): string {
    switch (intent) {
      case 'search': return this.t('scene.intentSearch');
      case 'recommend': return this.t('scene.intentRecommend');
      case 'details': return this.t('scene.intentDetails');
      case 'unknown': return this.t('scene.intentUnknown');
      default: return intent;
    }
  }

  /** Maps confidence levels to user-friendly labels. */
  confidenceLabel(confidence: string): string {
    switch (confidence) {
      case 'high': return this.t('scene.confidenceHigh');
      case 'medium': return this.t('scene.confidenceMedium');
      case 'low': return this.t('scene.confidenceLow');
      default: return '';
    }
  }
}
