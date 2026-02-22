import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location, DecimalPipe, SlicePipe } from '@angular/common';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { MovieDetail, CastMember } from '../../models/movie.model';

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [DecimalPipe, SlicePipe],
  template: `
    @if (movie(); as m) {
      <div class="detail-page">
        <div class="backdrop" [style.background-image]="'url(' + imageService.getBackdropUrl(m.backdrop_path, 'original') + ')'">
          <div class="backdrop-overlay"></div>
        </div>

        <div class="detail-content">
          <button class="back-btn" (click)="goBack()">&#8592; Retour</button>

          <div class="detail-header">
            <img
              [src]="imageService.getPosterUrl(m.poster_path, 'w500')"
              [alt]="m.title"
              class="detail-poster"
            />
            <div class="detail-info">
              <h1 class="title">{{ m.title }}</h1>

              @if (m.tagline) {
                <p class="tagline">{{ m.tagline }}</p>
              }

              <div class="meta">
                <span class="rating">
                  <span class="star">&#9733;</span>
                  {{ m.vote_average | number:'1.1-1' }}
                  <span class="vote-count">({{ m.vote_count }} votes)</span>
                </span>
                <span class="separator">&#8226;</span>
                <span>{{ m.release_date | slice:0:4 }}</span>
                @if (m.runtime) {
                  <span class="separator">&#8226;</span>
                  <span>{{ formatRuntime(m.runtime) }}</span>
                }
              </div>

              <div class="genres">
                @for (genre of m.genres; track genre.id) {
                  <span class="genre-tag">{{ genre.name }}</span>
                }
              </div>

              <div class="synopsis">
                <h3>Synopsis</h3>
                <p>{{ m.overview || 'Aucun synopsis disponible.' }}</p>
              </div>
            </div>
          </div>

          @if (topCast().length > 0) {
            <section class="cast-section">
              <h3>Casting principal</h3>
              <div class="cast-grid">
                @for (member of topCast(); track member.id) {
                  <div class="cast-card">
                    <img
                      [src]="imageService.getProfileUrl(member.profile_path)"
                      [alt]="member.name"
                      class="cast-photo"
                      loading="lazy"
                    />
                    <div class="cast-info">
                      <span class="cast-name">{{ member.name }}</span>
                      <span class="cast-character">{{ member.character }}</span>
                    </div>
                  </div>
                }
              </div>
            </section>
          }

          @if (director()) {
            <div class="director">
              <span class="director-label">Realise par</span>
              <span class="director-name">{{ director() }}</span>
            </div>
          }
        </div>
      </div>
    } @else {
      <div class="loader"><div class="spinner"></div></div>
    }
  `,
  styleUrl: './movie-detail.component.scss'
})
export class MovieDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private movieService = inject(MovieService);
  imageService = inject(ImageService);

  movie = signal<MovieDetail | null>(null);
  topCast = signal<CastMember[]>([]);
  director = signal<string>('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.movieService.getMovieDetail(id).subscribe(detail => {
      this.movie.set(detail);
      if (detail.credits?.cast) {
        this.topCast.set(detail.credits.cast.slice(0, 10));
      }
      if (detail.credits?.crew) {
        const dir = detail.credits.crew.find(c => c.job === 'Director');
        if (dir) this.director.set(dir.name);
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  /** Converts total minutes to "Xh Ymin" display format. */
  formatRuntime(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}min` : `${m}min`;
  }
}
