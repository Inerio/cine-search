import { Component, inject, OnInit, signal, effect, untracked, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location, DecimalPipe, SlicePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { MovieDetail, CastMember, CrewMember } from '../../models/movie.model';

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [DecimalPipe, SlicePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (movie(); as m) {
      <div class="detail-page">
        <div class="backdrop" [style.background-image]="'url(' + imageService.getBackdropUrl(m.backdrop_path, 'original') + ')'">
          <div class="backdrop-overlay"></div>
        </div>

        <div class="detail-content">
          <button class="back-btn" (click)="goBack()">&#8592; {{ t('detail.back') }}</button>

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
                  <span class="vote-count">({{ m.vote_count }} {{ t('detail.votes') }})</span>
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
                <h3>{{ t('detail.synopsis') }}</h3>
                <p>{{ m.overview || t('detail.noSynopsis') }}</p>
              </div>
            </div>
          </div>

          @if (topCast().length > 0) {
            <section class="cast-section">
              <h3>{{ t('detail.cast') }}</h3>
              <div class="cast-grid">
                @for (member of topCast(); track member.id) {
                  <div class="cast-card clickable" (click)="goToActor(member)">
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

          @if (directorInfo()) {
            <div class="director clickable" (click)="goToDirector()">
              <span class="director-label">{{ t('detail.directedBy') }}</span>
              <span class="director-name">{{ directorInfo()!.name }}</span>
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
  private router = inject(Router);
  private location = inject(Location);
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  private destroyRef = inject(DestroyRef);
  imageService = inject(ImageService);

  movie = signal<MovieDetail | null>(null);
  topCast = signal<CastMember[]>([]);
  directorInfo = signal<CrewMember | null>(null);

  private movieId = 0;
  private activeRequest?: Subscription;

  /** Re-fetches movie data whenever the language changes. */
  private langEffect = effect(() => {
    this.ts.lang(); // track language signal
    if (this.movieId > 0) {
      untracked(() => this.loadMovie(this.movieId));
    }
  });

  t(key: string): string { return this.ts.t(key); }

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => this.activeRequest?.unsubscribe());

    this.movieId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadMovie(this.movieId);
  }

  private loadMovie(id: number): void {
    this.activeRequest?.unsubscribe();
    this.activeRequest = this.movieService.getMovieDetail(id).subscribe(detail => {
      this.movie.set(detail);
      if (detail.credits?.cast) {
        this.topCast.set(detail.credits.cast.slice(0, 10));
      }
      if (detail.credits?.crew) {
        const dir = detail.credits.crew.find(c => c.job === 'Director');
        this.directorInfo.set(dir ?? null);
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  goToActor(member: CastMember): void {
    this.router.navigate(['/search'], {
      queryParams: { tab: 'actor', personId: member.id }
    });
  }

  goToDirector(): void {
    const dir = this.directorInfo();
    if (dir) {
      this.router.navigate(['/search'], {
        queryParams: { tab: 'director', personId: dir.id }
      });
    }
  }

  formatRuntime(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}min` : `${m}min`;
  }
}
