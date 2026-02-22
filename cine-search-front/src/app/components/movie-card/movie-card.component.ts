import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { Movie } from '../../models/movie.model';
import { ImageService } from '../../services/image.service';
import { DecimalPipe, SlicePipe } from '@angular/common';

@Component({
  selector: 'app-movie-card',
  standalone: true,
  imports: [DecimalPipe, SlicePipe],
  template: `
    <article class="movie-card" (click)="goToDetail()">
      <div class="poster-wrapper">
        <img
          [src]="imageService.getPosterUrl(movie().poster_path)"
          [alt]="movie().title"
          class="poster"
          loading="lazy"
        />
        <div class="overlay">
          <div class="rating">
            <span class="star">&#9733;</span>
            {{ movie().vote_average | number:'1.1-1' }}
          </div>
          <p class="overview">{{ movie().overview | slice:0:120 }}{{ movie().overview.length > 120 ? '...' : '' }}</p>
          <button class="detail-btn">Voir details</button>
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

  goToDetail(): void {
    this.router.navigate(['/movie', this.movie().id]);
  }
}
