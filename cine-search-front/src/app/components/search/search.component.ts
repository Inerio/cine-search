import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { SceneSearchComponent } from '../scene-search/scene-search.component';
import { ActorResultsComponent } from '../actor-results/actor-results.component';
import { MovieService } from '../../services/movie.service';
import { Movie, Genre } from '../../models/movie.model';

type SearchTab = 'movie' | 'actor' | 'scene';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, MovieCardComponent, SceneSearchComponent, ActorResultsComponent],
  template: `
    <div class="search-page">
      <div class="tabs">
        <button
          class="tab"
          [class.active]="activeTab() === 'movie'"
          (click)="setTab('movie')">
          🎬 Film
        </button>
        <button
          class="tab"
          [class.active]="activeTab() === 'actor'"
          (click)="setTab('actor')">
          🎭 Acteur
        </button>
        <button
          class="tab"
          [class.active]="activeTab() === 'scene'"
          (click)="setTab('scene')">
          ✨ Scène / IA
        </button>
      </div>

      @if (activeTab() === 'movie') {
        <div class="search-section">
          <div class="search-input-row">
            <input
              type="text"
              [ngModel]="movieQuery()"
              (ngModelChange)="movieQuery.set($event)"
              (keyup.enter)="searchMovies()"
              placeholder="Nom du film..."
              class="input"
            />
            <button class="btn-primary" (click)="searchMovies()" [disabled]="loading()">Rechercher</button>
          </div>

          <div class="filters">
            <select [ngModel]="selectedGenre()" (ngModelChange)="selectedGenre.set($event)" class="select">
              <option [ngValue]="null">Tous les genres</option>
              @for (genre of genres(); track genre.id) {
                <option [ngValue]="genre.id">{{ genre.name }}</option>
              }
            </select>
            <input
              type="number"
              [ngModel]="selectedYear()"
              (ngModelChange)="selectedYear.set($event)"
              placeholder="Année"
              class="input input-small"
              min="1900"
              max="2026"
            />
            <select [ngModel]="selectedRating()" (ngModelChange)="selectedRating.set($event)" class="select">
              <option [ngValue]="null">Note min.</option>
              <option [ngValue]="7">7+</option>
              <option [ngValue]="8">8+</option>
              <option [ngValue]="9">9+</option>
            </select>
            <button class="btn-secondary" (click)="applyFilters()" [disabled]="loading()">Filtrer</button>
          </div>

          @if (loading()) {
            <div class="loader">
              <div class="spinner"></div>
            </div>
          }

          @if (movieResults().length > 0) {
            <div class="results-count">{{ totalResults() }} résultats trouvés</div>
            <div class="movie-grid">
              @for (movie of movieResults(); track movie.id) {
                <app-movie-card [movie]="movie" />
              }
            </div>
          }

          @if (!loading() && searched() && movieResults().length === 0) {
            <div class="empty-state">
              <span class="empty-icon">🔍</span>
              <p>Aucun film trouvé</p>
            </div>
          }
        </div>
      }

      @if (activeTab() === 'actor') {
        <app-actor-results />
      }

      @if (activeTab() === 'scene') {
        <app-scene-search />
      }
    </div>
  `,
  styleUrl: './search.component.scss'
})
export class SearchComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private movieService = inject(MovieService);

  activeTab = signal<SearchTab>('movie');
  movieQuery = signal('');
  movieResults = signal<Movie[]>([]);
  genres = signal<Genre[]>([]);
  selectedGenre = signal<number | null>(null);
  selectedYear = signal<number | null>(null);
  selectedRating = signal<number | null>(null);
  loading = signal(false);
  searched = signal(false);
  totalResults = signal(0);

  ngOnInit(): void {
    this.movieService.getGenres().subscribe(res => {
      this.genres.set(res.genres);
    });

    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab.set(params['tab'] as SearchTab);
      }
      if (params['q']) {
        this.movieQuery.set(params['q']);
        if (this.activeTab() === 'movie') {
          this.searchMovies();
        }
      }
    });
  }

  setTab(tab: SearchTab): void {
    this.activeTab.set(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge'
    });
  }

  searchMovies(): void {
    const query = this.movieQuery().trim();
    if (!query || this.loading()) return;

    this.loading.set(true);
    this.searched.set(true);
    this.movieService.searchMovies(query).subscribe({
      next: res => {
        this.movieResults.set(res.results);
        this.totalResults.set(res.total_results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilters(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.searched.set(true);
    this.movieService.discoverMovies({
      genreId: this.selectedGenre() ?? undefined,
      year: this.selectedYear() ?? undefined,
      minRating: this.selectedRating() ?? undefined
    }).subscribe({
      next: res => {
        this.movieResults.set(res.results);
        this.totalResults.set(res.total_results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
