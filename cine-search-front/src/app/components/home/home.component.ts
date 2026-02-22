import { Component, inject, OnInit, signal } from '@angular/core';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { Movie } from '../../models/movie.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MovieCardComponent],
  template: `
    <section class="hero" [style.background-image]="heroBackdrop()">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">Découvrez le cinéma autrement</h1>
        <p class="hero-subtitle">
          Recherchez par titre, acteur, ou décrivez une scène pour trouver le film parfait.
        </p>
        <div class="hero-actions">
          <button class="hero-cta" (click)="goToExplore()">Explorer les films</button>
          <button class="hero-cta-secondary" (click)="goToSceneSearch()">Recherche avancée</button>
        </div>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">
        <span class="title-accent"></span>
        Tendances de la semaine
      </h2>
      <div class="movie-grid">
        @for (movie of trendingMovies(); track movie.id) {
          <app-movie-card [movie]="movie" />
        }
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">
        <span class="title-accent"></span>
        Films populaires
      </h2>
      <div class="movie-grid">
        @for (movie of popularMovies(); track movie.id) {
          <app-movie-card [movie]="movie" />
        }
      </div>
    </section>
  `,
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private movieService = inject(MovieService);
  private imageService = inject(ImageService);
  private router = inject(Router);

  trendingMovies = signal<Movie[]>([]);
  popularMovies = signal<Movie[]>([]);
  heroBackdrop = signal<string>('');

  ngOnInit(): void {
    this.movieService.getTrending().subscribe(res => {
      this.trendingMovies.set(res.results.slice(0, 12));
      if (res.results.length > 0) {
        const backdrop = this.imageService.getBackdropUrl(res.results[0].backdrop_path, 'original');
        this.heroBackdrop.set(`url(${backdrop})`);
      }
    });

    this.movieService.getPopular().subscribe(res => {
      this.popularMovies.set(res.results.slice(0, 12));
    });
  }

  goToExplore(): void {
    this.router.navigate(['/search']);
  }

  goToSceneSearch(): void {
    this.router.navigate(['/search'], { queryParams: { tab: 'scene' } });
  }
}
