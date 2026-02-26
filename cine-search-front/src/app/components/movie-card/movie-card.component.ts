import { Component, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Movie } from '../../models/movie.model';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { DecimalPipe, SlicePipe } from '@angular/common';

@Component({
  selector: 'app-movie-card',
  standalone: true,
  imports: [DecimalPipe, SlicePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="movie-card" (click)="goToDetail()">
      <div class="poster-wrapper">
        <img
          [src]="imageService.getPosterUrl(movie().poster_path)"
          [alt]="movie().title"
          class="poster"
          loading="lazy"
        />
        <div class="rating-badge">
          <span class="star">&#9733;</span>
          {{ movie().vote_average | number:'1.1-1' }}
        </div>
        <div class="overlay">
          <div class="rating">
            <span class="star">&#9733;</span>
            {{ movie().vote_average | number:'1.1-1' }}
          </div>
          <p class="overview">{{ movie().overview | slice:0:120 }}{{ movie().overview.length > 120 ? '...' : '' }}</p>
          <button class="detail-btn">{{ t('card.viewDetails') }}</button>
        </div>
      </div>
      <div class="info">
        <h3 class="title">{{ movie().title }}</h3>
        <span class="year">{{ movie().release_date | slice:0:4 }}</span>
      </div>
    </article>
  `,
  styleUrl: './movie-card.component.scss'
})
export class MovieCardComponent {
  movie = input.required<Movie>();
  imageService = inject(ImageService);
  private router = inject(Router);
  private ts = inject(TranslationService);

  t(key: string): string { return this.ts.t(key); }

  goToDetail(): void {
    this.router.navigate(['/movie', this.movie().id]);
  }
}
