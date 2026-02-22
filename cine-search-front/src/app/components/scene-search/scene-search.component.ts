import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { Movie, AiMovieQuery } from '../../models/movie.model';

@Component({
  selector: 'app-scene-search',
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  template: `
    <div class="scene-search">
      <div class="advanced-banner">
        <h2>Recherche avancee</h2>
        <p>Decrivez ce que vous cherchez en langage naturel — les resultats s'adaptent a votre demande.</p>
      </div>

      <div class="prompt-area">
        <textarea
          [ngModel]="description()"
          (ngModelChange)="description.set($event)"
          (keydown)="onKeydown($event)"
          placeholder="Ex: Un film de science-fiction sombre comme Interstellar, des comedies francaises recentes, le dernier film avec Tom Hanks..."
          class="textarea"
          rows="4"
        ></textarea>
        <div class="prompt-footer">
          <span class="hint">Ctrl + Entree pour rechercher</span>
          <button
            class="btn-search"
            (click)="search()"
            [disabled]="loading() || description().trim().length < 2">
            @if (loading()) {
              <span class="spinner-small"></span>
              Analyse en cours...
            } @else {
              Rechercher
            }
          </button>
        </div>
      </div>

      @if (loading()) {
        <div class="search-loading">
          <div class="pulse-ring"></div>
          <p>Analyse en cours...</p>
        </div>
      }

      @if (parsedQuery() && !loading()) {
        <div class="parsed-results">
          <span class="parsed-badge">{{ intentLabel(parsedQuery()!.intent) }}</span>
          @if (parsedQuery()!.genres && parsedQuery()!.genres!.length > 0) {
            @for (genre of parsedQuery()!.genres!; track genre) {
              <span class="parsed-tag">{{ genre }}</span>
            }
          }
          @if (parsedQuery()!.year) {
            <span class="parsed-tag">{{ parsedQuery()!.year }}</span>
          }
          @if (parsedQuery()!.query) {
            <span class="parsed-query">{{ parsedQuery()!.query }}</span>
          }
        </div>
      }

      @if (results().length > 0) {
        <div class="results-header">
          <h3>{{ results().length }} films trouves</h3>
        </div>
        <div class="movie-grid">
          @for (movie of results(); track movie.id) {
            <app-movie-card [movie]="movie" />
          }
        </div>
      }

      @if (!loading() && searched() && results().length === 0) {
        <div class="empty-state">
          @if (parsedQuery()?.intent === 'unknown') {
            <p>Votre demande ne semble pas liee au cinema. Essayez autre chose !</p>
          } @else {
            <p>Aucune correspondance exacte trouvee.<br>
            Essayez avec le nom d'un acteur, un titre partiel ou plus de details !</p>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './scene-search.component.scss'
})
export class SceneSearchComponent {
  private movieService = inject(MovieService);

  description = signal('');
  results = signal<Movie[]>([]);
  parsedQuery = signal<AiMovieQuery | null>(null);
  loading = signal(false);
  searched = signal(false);

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

    this.movieService.aiParse(desc).subscribe({
      next: response => {
        this.parsedQuery.set(response.parsed);
        this.results.set(response.results);
        this.loading.set(false);
      },
      error: () => {
        this.results.set([]);
        this.parsedQuery.set(null);
        this.loading.set(false);
      }
    });
  }

  /** Maps intent codes to user-friendly French labels. */
  intentLabel(intent: string): string {
    switch (intent) {
      case 'search': return 'Recherche';
      case 'recommend': return 'Recommandation';
      case 'details': return 'Details';
      case 'unknown': return 'Non reconnu';
      default: return intent;
    }
  }
}
