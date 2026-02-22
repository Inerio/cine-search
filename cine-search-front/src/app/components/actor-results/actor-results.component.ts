import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
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
      </div>

      <!-- Actor grid: keeps previous results visible during loading (opacity fade) -->
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
            <h3 class="default-title">Acteurs populaires</h3>
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
          <div class="empty-state"><p>Aucun acteur trouve</p></div>
        }
      </div>
    </div>
  `,
  styleUrl: './actor-results.component.scss'
})
export class ActorResultsComponent implements OnInit, OnDestroy {
  private movieService = inject(MovieService);
  imageService = inject(ImageService);

  // State
  actorQuery = signal('');
  actors = signal<Person[]>([]);
  defaultActors = signal<Person[]>([]);
  selectedActor = signal<Person | null>(null);
  actorMovies = signal<Movie[]>([]);
  loading = signal(false);
  searched = signal(false);

  // Internal
  private searchTimeout: any;
  private activeRequest?: Subscription;

  ngOnInit(): void {
    this.loadPopularActors();
  }

  ngOnDestroy(): void {
    clearTimeout(this.searchTimeout);
    this.activeRequest?.unsubscribe();
  }

  // Load popular actors as default content
  private loadPopularActors(): void {
    this.movieService.getPopularActors().subscribe({
      next: res => {
        const actors = res.results
          .filter(p => p.profile_path)
          .slice(0, 20);
        this.defaultActors.set(actors);
      },
      error: () => {}
    });
  }

  // Debounced text input handler (400ms)
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

  // Immediate search on Enter
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

  // Select an actor and load their filmography
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
