import { Component, inject, OnInit, signal, effect, untracked, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location, DecimalPipe, SlicePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { TvDetail, CastMember, WatchProviders } from '../../models/movie.model';

@Component({
  selector: 'app-tv-detail',
  standalone: true,
  imports: [DecimalPipe, SlicePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (show(); as s) {
      <div class="detail-page">
        <div class="backdrop" [style.background-image]="'url(' + imageService.getBackdropUrl(s.backdrop_path, 'original') + ')'">
          <div class="backdrop-overlay"></div>
        </div>

        <div class="detail-content">
          <button class="back-btn" (click)="goBack()">&#8592; {{ t('tvDetail.back') }}</button>

          <div class="detail-header">
            <img
              [src]="imageService.getPosterUrl(s.poster_path, 'w500')"
              [alt]="s.name"
              class="detail-poster"
            />
            <div class="detail-info">
              <h1 class="title">{{ s.name }}</h1>

              @if (s.tagline) {
                <p class="tagline">{{ s.tagline }}</p>
              }

              <div class="meta">
                <span class="rating">
                  <span class="star">&#9733;</span>
                  {{ s.vote_average | number:'1.1-1' }}
                  <span class="vote-count">({{ s.vote_count }} {{ t('tvDetail.votes') }})</span>
                </span>
                <span class="separator">&#8226;</span>
                <span>{{ s.first_air_date | slice:0:4 }}</span>
                @if (s.last_air_date && s.last_air_date !== s.first_air_date) {
                  <span>- {{ s.last_air_date | slice:0:4 }}</span>
                }
                <span class="separator">&#8226;</span>
                <span>{{ s.number_of_seasons }} {{ t('tvDetail.seasonCount') }}</span>
                <span class="separator">&#8226;</span>
                <span>{{ s.number_of_episodes }} {{ t('tvDetail.episodeCount') }}</span>
                @if (avgRuntime() > 0) {
                  <span class="separator">&#8226;</span>
                  <span>~{{ avgRuntime() }}min/ep</span>
                }
              </div>

              <div class="genres">
                @for (genre of s.genres; track genre.id) {
                  <span class="genre-tag">{{ genre.name }}</span>
                }
              </div>

              <div class="synopsis">
                <h3>{{ t('tvDetail.synopsis') }}</h3>
                <p>{{ s.overview || t('tvDetail.noSynopsis') }}</p>
              </div>
            </div>
          </div>

          @if (s.created_by && s.created_by.length > 0) {
            <div class="creators">
              <span class="creators-label">{{ t('tvDetail.createdBy') }}</span>
              @for (creator of s.created_by; track creator.id; let last = $last) {
                <span class="creator-name">{{ creator.name }}{{ last ? '' : ', ' }}</span>
              }
            </div>
          }

          @if (s.networks && s.networks.length > 0) {
            <section class="networks-section">
              <h3>{{ t('tvDetail.networks') }}</h3>
              <div class="networks-grid">
                @for (network of s.networks; track network.id) {
                  <div class="network-card">
                    @if (network.logo_path) {
                      <img
                        [src]="imageService.getLogoUrl(network.logo_path)"
                        [alt]="network.name"
                        class="network-logo"
                        loading="lazy"
                      />
                    } @else {
                      <span class="network-name-only">{{ network.name }}</span>
                    }
                  </div>
                }
              </div>
            </section>
          }

          @if (topCast().length > 0) {
            <section class="cast-section">
              <h3>{{ t('tvDetail.cast') }}</h3>
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

          @if (s.seasons && s.seasons.length > 0) {
            <section class="seasons-section">
              <h3>{{ t('tvDetail.seasons') }}</h3>
              <div class="seasons-grid">
                @for (season of s.seasons; track season.id) {
                  @if (season.season_number > 0) {
                    <div class="season-card">
                      <img
                        [src]="imageService.getPosterUrl(season.poster_path, 'w185')"
                        [alt]="season.name"
                        class="season-poster"
                        loading="lazy"
                      />
                      <div class="season-info">
                        <span class="season-name">{{ season.name }}</span>
                        <span class="season-meta">{{ season.episode_count }} {{ t('tvDetail.episodes') }}</span>
                        @if (season.air_date) {
                          <span class="season-date">{{ season.air_date | slice:0:4 }}</span>
                        }
                      </div>
                    </div>
                  }
                }
              </div>
            </section>
          }

          @if (watchProviders(); as wp) {
            <section class="watch-providers">
              <h3>{{ t('tvDetail.whereToWatch') }}</h3>
              @if (wp.flatrate?.length) {
                <div class="provider-group">
                  <span class="provider-label">{{ t('tvDetail.streaming') }}</span>
                  <div class="provider-logos">
                    @for (p of wp.flatrate; track p.provider_id) {
                      <img [src]="imageService.getLogoUrl(p.logo_path)" [alt]="p.provider_name" [title]="p.provider_name" class="provider-logo" />
                    }
                  </div>
                </div>
              }
              @if (wp.rent?.length) {
                <div class="provider-group">
                  <span class="provider-label">{{ t('tvDetail.rent') }}</span>
                  <div class="provider-logos">
                    @for (p of wp.rent; track p.provider_id) {
                      <img [src]="imageService.getLogoUrl(p.logo_path)" [alt]="p.provider_name" [title]="p.provider_name" class="provider-logo" />
                    }
                  </div>
                </div>
              }
              @if (wp.buy?.length) {
                <div class="provider-group">
                  <span class="provider-label">{{ t('tvDetail.buy') }}</span>
                  <div class="provider-logos">
                    @for (p of wp.buy; track p.provider_id) {
                      <img [src]="imageService.getLogoUrl(p.logo_path)" [alt]="p.provider_name" [title]="p.provider_name" class="provider-logo" />
                    }
                  </div>
                </div>
              }
              <div class="provider-footer">
                <a [href]="getWatchSearchUrl()" target="_blank" rel="noopener" class="no-providers-link">
                  <span class="search-icon">&#128269;</span>
                  {{ t('tvDetail.noProviders') }}
                </a>
                @if (wp.link) {
                  <a [href]="wp.link" target="_blank" rel="noopener" class="provider-credit">
                    {{ t('tvDetail.poweredByJustWatch') }}
                  </a>
                }
              </div>
            </section>
          }
        </div>
      </div>
    } @else {
      <div class="loader"><div class="spinner"></div></div>
    }
  `,
  styleUrl: './tv-detail.component.scss'
})
export class TvDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  private destroyRef = inject(DestroyRef);
  imageService = inject(ImageService);

  show = signal<TvDetail | null>(null);
  topCast = signal<CastMember[]>([]);
  watchProviders = signal<WatchProviders | null>(null);
  avgRuntime = signal(0);

  private tvId = 0;
  private activeRequest?: Subscription;
  private watchSub?: Subscription;

  private langEffect = effect(() => {
    this.ts.lang();
    if (this.tvId > 0) {
      untracked(() => this.loadTv(this.tvId));
    }
  });

  t(key: string): string { return this.ts.t(key); }

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => {
      this.activeRequest?.unsubscribe();
      this.watchSub?.unsubscribe();
    });

    this.tvId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadTv(this.tvId);
  }

  private loadTv(id: number): void {
    this.activeRequest?.unsubscribe();
    this.activeRequest = this.movieService.getTvDetail(id).subscribe(detail => {
      this.show.set(detail);
      if (detail.credits?.cast) {
        this.topCast.set(detail.credits.cast.slice(0, 10));
      }
      if (detail.episode_run_time && detail.episode_run_time.length > 0) {
        const avg = Math.round(detail.episode_run_time.reduce((a, b) => a + b, 0) / detail.episode_run_time.length);
        this.avgRuntime.set(avg);
      } else {
        this.avgRuntime.set(0);
      }
    });
    this.watchSub?.unsubscribe();
    this.watchSub = this.movieService.getTvWatchProviders(id).subscribe({
      next: wp => this.watchProviders.set(wp),
      error: () => this.watchProviders.set(null)
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

  getWatchSearchUrl(): string {
    const s = this.show();
    if (!s) return '#';
    const keyword = this.ts.lang() === 'fr' ? 'regarder' : 'watch';
    const query = encodeURIComponent(`${keyword} ${s.name} streaming`);
    return `https://www.google.com/search?q=${query}`;
  }
}
