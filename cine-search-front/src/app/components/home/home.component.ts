import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { Movie } from '../../models/movie.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MovieCardComponent],
  template: `
    <section class="hero">
      <!-- Two stacked backdrop layers for smooth crossfade -->
      <div
        class="hero-bg"
        [class.visible]="activeBg() === 0"
        [style.background-image]="backdrop0()"
      ></div>
      <div
        class="hero-bg"
        [class.visible]="activeBg() === 1"
        [style.background-image]="backdrop1()"
      ></div>

      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">{{ t('home.heroTitle') }}</h1>
        <p class="hero-subtitle">{{ t('home.heroSubtitle') }}</p>
        <div class="hero-actions">
          <button class="hero-cta" (click)="goToExplore()">{{ t('home.exploreCta') }}</button>
          <button class="hero-cta-secondary" (click)="goToSceneSearch()">{{ t('home.advancedCta') }}</button>
        </div>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">
        <span class="title-accent"></span>
        {{ t('home.trendingSection') }}
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
        {{ t('home.popularSection') }}
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
export class HomeComponent implements OnInit, OnDestroy {
  private movieService = inject(MovieService);
  private imageService = inject(ImageService);
  private router = inject(Router);
  private ts = inject(TranslationService);

  trendingMovies = signal<Movie[]>([]);
  popularMovies = signal<Movie[]>([]);

  // --- Backdrop slideshow ---
  backdrop0 = signal('');
  backdrop1 = signal('');
  activeBg = signal<0 | 1>(0);

  private backdropUrls: string[] = [];
  private currentIndex = 0;
  private slideshowInterval: any;

  private static readonly SLIDE_DURATION = 8000;

  t(key: string): string { return this.ts.t(key); }

  ngOnInit(): void {
    this.movieService.getTrending().subscribe(res => {
      this.trendingMovies.set(res.results.slice(0, 20));
      this.initSlideshow(res.results);
    });

    this.movieService.getPopular().subscribe(res => {
      this.popularMovies.set(res.results.slice(0, 20));
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.slideshowInterval);
  }

  /** Shuffles backdrops, sets the first one, and starts the cycle. */
  private initSlideshow(movies: Movie[]): void {
    this.backdropUrls = movies
      .filter(m => m.backdrop_path)
      .map(m => this.imageService.getBackdropUrl(m.backdrop_path, 'original'));

    if (this.backdropUrls.length === 0) return;

    // Shuffle for a random start order
    this.shuffle(this.backdropUrls);

    // Set the first backdrop immediately on layer 0
    this.backdrop0.set(`url(${this.backdropUrls[0]})`);
    this.activeBg.set(0);
    this.currentIndex = 0;

    if (this.backdropUrls.length < 2) return;

    // Preload the next image into the hidden layer
    this.backdrop1.set(`url(${this.backdropUrls[1]})`);

    this.slideshowInterval = setInterval(() => this.nextSlide(), HomeComponent.SLIDE_DURATION);
  }

  /** Crossfades to the next backdrop image. */
  private nextSlide(): void {
    this.currentIndex = (this.currentIndex + 1) % this.backdropUrls.length;
    const nextIndex = (this.currentIndex + 1) % this.backdropUrls.length;

    if (this.activeBg() === 0) {
      this.activeBg.set(1);
      setTimeout(() => this.backdrop0.set(`url(${this.backdropUrls[nextIndex]})`), 1500);
    } else {
      this.activeBg.set(0);
      setTimeout(() => this.backdrop1.set(`url(${this.backdropUrls[nextIndex]})`), 1500);
    }
  }

  /** Fisher-Yates shuffle. */
  private shuffle(arr: string[]): void {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
  }

  goToExplore(): void {
    this.router.navigate(['/search']);
  }

  goToSceneSearch(): void {
    this.router.navigate(['/search'], { queryParams: { tab: 'scene' } });
  }
}
