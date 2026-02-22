import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
      <div class="search-input-row">
        <input
          type="text"
          [ngModel]="actorQuery()"
          (ngModelChange)="actorQuery.set($event)"
          (keyup.enter)="searchActors()"
          placeholder="Nom de l'acteur..."
          class="input"
        />
        <button class="btn-primary" (click)="searchActors()" [disabled]="loading()">Rechercher</button>
      </div>

      @if (loading()) {
        <div class="loader"><div class="spinner"></div></div>
      }

      @if (actors().length > 0 && !selectedActor()) {
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

      @if (selectedActor()) {
        <div class="selected-actor">
          <button class="back-btn" (click)="clearSelection()">← Retour</button>
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

      @if (!loading() && searched() && actors().length === 0) {
        <div class="empty-state">
          <p>Aucun acteur trouvé</p>
        </div>
      }
    </div>
  `,
  styleUrl: './actor-results.component.scss'
})
export class ActorResultsComponent {
  private movieService = inject(MovieService);
  imageService = inject(ImageService);

  actorQuery = signal('');
  actors = signal<Person[]>([]);
  selectedActor = signal<Person | null>(null);
  actorMovies = signal<Movie[]>([]);
  loading = signal(false);
  searched = signal(false);

  searchActors(): void {
    const query = this.actorQuery().trim();
    if (!query || this.loading()) return;

    this.loading.set(true);
    this.searched.set(true);
    this.selectedActor.set(null);
    this.movieService.searchPersons(query).subscribe({
      next: res => {
        this.actors.set(res.results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  selectActor(actor: Person): void {
    if (this.loading()) return;
    this.selectedActor.set(actor);
    this.loading.set(true);
    this.movieService.getPersonMovies(actor.id).subscribe({
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
