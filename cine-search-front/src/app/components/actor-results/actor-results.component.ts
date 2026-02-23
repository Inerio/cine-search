import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Subscription } from 'rxjs';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { Movie, Person } from '../../models/movie.model';

@Component({
  selector: 'app-actor-results',
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  template: `
    <div class="actor-search">
      <div class="search-row">
        <input
          type="text"
          [ngModel]="actorQuery()"
          (ngModelChange)="onQueryInput($event)"
          (keyup.enter)="searchNow()"
          placeholder="Rechercher un acteur..."
          class="input search-input"
        />
        <button class="btn-trending" (click)="showTrending()" title="Tendances du moment">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          Tendances
        </button>
      </div>

      <!-- Results area: opacity fade during loading to prevent layout jump -->
      <div class="results-area" [class.is-loading]="loading()">

        @if (selectedActor()) {
          <div class="selected-actor">
            <button class="back-btn" (click)="clearSelection()">&#8592; Retour</button>
            <div class="actor-header">
              <img
                [src]="imageService.getProfileUrl(selectedActor()!.profile_path, 'w342')"
                [alt]="selectedActor()!.name"
                class="actor-photo-large"
              />
              <div>
                <h2>{{ selectedActor()!.name }}</h2>
                <p class="filmography-count">{{ actorMovies().length }} films</p>
              </div>
            </div>
            <div class="movie-grid">
              @for (movie of actorMovies(); track movie.id) {
                <app-movie-card [movie]="movie" />
              }
            </div>
          </div>
        }

        @if (!selectedActor() && actors().length > 0) {
          <div class="actor-grid">
            @for (actor of actors(); track actor.id) {
              <div class="actor-card" (click)="selectActor(actor)">
                <img
                  [src]="imageService.getProfileUrl(actor.profile_path)"
                  [alt]="actor.name"
                  class="actor-photo"
                />
                <div class="actor-info">
                  <h3>{{ actor.name }}</h3>
                  <span class="department">{{ actor.known_for_department }}</span>
                </div>
              </div>
            }
          </div>
        }

        @if (!selectedActor() && !searched() && defaultActors().length > 0 && actors().length === 0) {
          <div class="default-section">
            <h3 class="default-title">Acteurs du moment</h3>
            <div class="actor-grid">
              @for (actor of defaultActors(); track actor.id) {
                <div class="actor-card" (click)="selectActor(actor)">
                  <img
                    [src]="imageService.getProfileUrl(actor.profile_path)"
                    [alt]="actor.name"
                    class="actor-photo"
                  />
                  <div class="actor-info">
                    <h3>{{ actor.name }}</h3>
                    <span class="department">{{ actor.known_for_department }}</span>
                  </div>
                </div>
              }
            </div>
          </div>
        }

        @if (loading() && actors().length === 0 && !selectedActor() && defaultActors().length === 0) {
          <div class="loader"><div class="spinner"></div></div>
        }

        @if (!loading() && searched() && actors().length === 0 && !selectedActor()) {
          <div class="empty-state"><p>Aucun acteur trouvé</p></div>
        }
      </div>
    </div>
  `,
  styleUrl: './actor-results.component.scss'
})
export class ActorResultsComponent implements OnInit, OnDestroy {
  private movieService = inject(MovieService);
  imageService = inject(ImageService);

  // --- State ---
  actorQuery = signal('');
  actors = signal<Person[]>([]);
  defaultActors = signal<Person[]>([]);
  selectedActor = signal<Person | null>(null);
  actorMovies = signal<Movie[]>([]);
  loading = signal(false);
  searched = signal(false);

  // --- Internal ---
  private searchTimeout: any;
  private activeRequest?: Subscription;

  ngOnInit(): void {
    this.loadTrendingActors();
  }

  ngOnDestroy(): void {
    clearTimeout(this.searchTimeout);
    this.activeRequest?.unsubscribe();
  }

  // =====================
  //  Default content
  // =====================

  /** Loads 4 pages of trending persons in parallel to display 60+ actors. */
  private loadTrendingActors(): void {
    forkJoin([
      this.movieService.getTrendingActors(1),
      this.movieService.getTrendingActors(2),
      this.movieService.getTrendingActors(3),
      this.movieService.getTrendingActors(4),
    ]).subscribe({
      next: pages => {
        const all = pages.flatMap(p => p.results);
        // Deduplicate by id, keep only actors with a photo
        const seen = new Set<number>();
        const actors = all.filter(p => {
          if (seen.has(p.id) || !p.profile_path || p.known_for_department !== 'Acting') return false;
          seen.add(p.id);
          return true;
        });
        this.defaultActors.set(actors);
      },
      error: () => {}
    });
  }

  // =====================
  //  Trending / Reset
  // =====================

  /** Resets search state and reloads trending actors. */
  showTrending(): void {
    this.actorQuery.set('');
    this.actors.set([]);
    this.searched.set(false);
    this.selectedActor.set(null);
    this.actorMovies.set([]);
    this.loadTrendingActors();
  }

  // =====================
  //  Debounced text search
  // =====================

  /** Handles text input with 400ms debounce. */
  onQueryInput(value: string): void {
    this.actorQuery.set(value);
    clearTimeout(this.searchTimeout);

    if (this.selectedActor()) {
      this.selectedActor.set(null);
      this.actorMovies.set([]);
    }

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.searched()) {
        this.searched.set(false);
        this.actors.set([]);
      }
      return;
    }

    this.searchTimeout = setTimeout(() => this.executeSearch(trimmed), 400);
  }

  /** Immediate search triggered by Enter key. */
  searchNow(): void {
    clearTimeout(this.searchTimeout);
    const trimmed = this.actorQuery().trim();
    if (trimmed.length >= 2) this.executeSearch(trimmed);
  }

  private executeSearch(query: string): void {
    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.searched.set(true);
    this.selectedActor.set(null);

    this.activeRequest = this.movieService.searchPersons(query).subscribe({
      next: res => {
        this.actors.set(res.results.filter(p => p.profile_path));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // =====================
  //  Actor selection
  // =====================

  /** Selects an actor and loads their full filmography. */
  selectActor(actor: Person): void {
    this.selectedActor.set(actor);
    this.loading.set(true);

    this.activeRequest?.unsubscribe();
    this.activeRequest = this.movieService.getPersonMovies(actor.id).subscribe({
      next: res => {
        const sorted = [...res.cast]
          .filter(m => m.poster_path)
          .sort((a, b) => b.popularity - a.popularity);
        this.actorMovies.set(sorted);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  clearSelection(): void {
    this.selectedActor.set(null);
    this.actorMovies.set([]);
  }
}
