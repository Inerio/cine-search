import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, forkJoin, Subscription } from 'rxjs';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { Movie, Person, PersonSearchResponse } from '../../models/movie.model';

// TMDB genre IDs for filtering actors by their known_for movies
const GENRE_IDS: Record<string, number> = {
  action: 28, comedy: 35, drama: 18, horror: 27, scifi: 878,
  romance: 10749, thriller: 53, animation: 16, adventure: 12,
  fantasy: 14, crime: 80, family: 10751, documentary: 99,
  war: 10752, history: 36, music: 10402, western: 37, mystery: 9648
};

type ActorMode = 'popular' | 'trending' | 'search' | 'filter';

const PAGE_SIZE = 36;           // 9 columns × 4 rows
const TMDB_PER_PAGE = 2;        // 2 TMDB pages (40 actors) → display 36
const BATCH_SIZE = 10;          // Load 10 TMDB pages per batch (200 actors)
const MAX_TMDB_PAGES = 100;     // Safety cap: 2000 actors max

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
          [placeholder]="t('actor.placeholder')"
          class="input search-input"
        />
        <button class="btn-trending" (click)="showTrending()" [title]="t('actor.trending')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          {{ t('actor.trending') }}
        </button>
      </div>

      <!-- Filters -->
      <div class="filters">
        <select class="filter-select" [ngModel]="selectedGender()" (ngModelChange)="selectedGender.set(+$event); onFilterChange()">
          <option [ngValue]="0">{{ t('actor.filter.allGenders') }}</option>
          <option [ngValue]="2">{{ t('actor.filter.male') }}</option>
          <option [ngValue]="1">{{ t('actor.filter.female') }}</option>
          <option [ngValue]="3">{{ t('actor.filter.nonBinary') }}</option>
        </select>

        <select class="filter-select" [ngModel]="selectedGenre()" (ngModelChange)="selectedGenre.set($event); onFilterChange()">
          <option value="">{{ t('actor.filter.allGenres') }}</option>
          <option value="action">{{ t('actor.filter.genre.action') }}</option>
          <option value="comedy">{{ t('actor.filter.genre.comedy') }}</option>
          <option value="drama">{{ t('actor.filter.genre.drama') }}</option>
          <option value="horror">{{ t('actor.filter.genre.horror') }}</option>
          <option value="scifi">{{ t('actor.filter.genre.scifi') }}</option>
          <option value="romance">{{ t('actor.filter.genre.romance') }}</option>
          <option value="thriller">{{ t('actor.filter.genre.thriller') }}</option>
          <option value="animation">{{ t('actor.filter.genre.animation') }}</option>
          <option value="adventure">{{ t('actor.filter.genre.adventure') }}</option>
          <option value="fantasy">{{ t('actor.filter.genre.fantasy') }}</option>
          <option value="crime">{{ t('actor.filter.genre.crime') }}</option>
          <option value="family">{{ t('actor.filter.genre.family') }}</option>
          <option value="documentary">{{ t('actor.filter.genre.documentary') }}</option>
          <option value="war">{{ t('actor.filter.genre.war') }}</option>
          <option value="history">{{ t('actor.filter.genre.history') }}</option>
          <option value="music">{{ t('actor.filter.genre.music') }}</option>
          <option value="western">{{ t('actor.filter.genre.western') }}</option>
          <option value="mystery">{{ t('actor.filter.genre.mystery') }}</option>
        </select>

        <select class="filter-select" [ngModel]="selectedCountry()" (ngModelChange)="selectedCountry.set($event); onFilterChange()">
          <option value="">{{ t('actor.filter.allCountries') }}</option>
          <option value="en">{{ t('actor.filter.country.en') }}</option>
          <option value="fr">{{ t('actor.filter.country.fr') }}</option>
          <option value="ja">{{ t('actor.filter.country.ja') }}</option>
          <option value="ko">{{ t('actor.filter.country.ko') }}</option>
          <option value="es">{{ t('actor.filter.country.es') }}</option>
          <option value="de">{{ t('actor.filter.country.de') }}</option>
          <option value="it">{{ t('actor.filter.country.it') }}</option>
          <option value="pt">{{ t('actor.filter.country.pt') }}</option>
          <option value="hi">{{ t('actor.filter.country.hi') }}</option>
          <option value="zh">{{ t('actor.filter.country.zh') }}</option>
          <option value="ru">{{ t('actor.filter.country.ru') }}</option>
          <option value="sv">{{ t('actor.filter.country.sv') }}</option>
          <option value="tr">{{ t('actor.filter.country.tr') }}</option>
        </select>

        <select class="filter-select" [ngModel]="selectedSort()" (ngModelChange)="selectedSort.set($event); onFilterChange()">
          <option value="popularity">{{ t('actor.filter.sortPopularity') }}</option>
          <option value="nameAZ">{{ t('actor.filter.sortNameAZ') }}</option>
          <option value="nameZA">{{ t('actor.filter.sortNameZA') }}</option>
        </select>

        @if (hasActiveFilters()) {
          <button class="btn-reset" (click)="resetFilters()">{{ t('actor.reset') }}</button>
        }
      </div>

      <!-- Results area -->
      <div class="results-area" [class.is-loading]="loading() && displayedActors().length > 0">

        @if (selectedActor()) {
          <!-- Actor detail view -->
          <div class="selected-actor">
            <button class="back-btn" (click)="clearSelection()">&#8592; {{ t('actor.back') }}</button>
            <div class="actor-header">
              <img
                [src]="imageService.getProfileUrl(selectedActor()!.profile_path, 'w342')"
                [alt]="selectedActor()!.name"
                class="actor-photo-large"
              />
              <div>
                <h2>{{ selectedActor()!.name }}</h2>
                <p class="filmography-count">{{ actorMovies().length }} {{ t('actor.films') }}</p>
              </div>
            </div>
            <div class="movie-grid">
              @for (movie of actorMovies(); track movie.id) {
                <app-movie-card [movie]="movie" />
              }
            </div>
          </div>
        }

        @if (!selectedActor()) {
          <!-- Section header -->
          @if (displayedActors().length > 0 || batchLoading()) {
            <div class="results-info">
              <h3 class="default-title">{{ modeTitle() }}</h3>
              @if (totalResults() > 0) {
                <span class="results-count">{{ totalResults() }} {{ t('actor.resultsCount') }}</span>
              }
            </div>
          }

          <!-- Actor grid -->
          @if (displayedActors().length > 0) {
            <div class="actor-grid">
              @for (actor of displayedActors(); track actor.id) {
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

          <!-- Batch loading hint -->
          @if (batchLoading() && displayedActors().length > 0) {
            <div class="batch-loading-hint">{{ t('actor.loadingMore') }}</div>
          }

          <!-- Pagination -->
          @if (totalPages() > 1 && !loading()) {
            <div class="pagination">
              <button class="page-btn" [disabled]="currentPage() <= 1" (click)="goToPage(currentPage() - 1)">
                &#8592; {{ t('search.previous') }}
              </button>
              @for (p of visiblePages(); track $index) {
                @if (p === -1) {
                  <span class="page-ellipsis">...</span>
                } @else {
                  <button class="page-num" [class.active]="p === currentPage()" (click)="goToPage(p)">{{ p }}</button>
                }
              }
              <button class="page-btn" [disabled]="currentPage() >= totalPages()" (click)="goToPage(currentPage() + 1)">
                {{ t('search.next') }} &#8594;
              </button>
            </div>
          }

          <!-- Spinner (initial load) -->
          @if (loading() && displayedActors().length === 0 && !batchLoading()) {
            <div class="loader"><div class="spinner"></div></div>
          }

          <!-- Empty state -->
          @if (!loading() && !batchLoading() && displayedActors().length === 0) {
            <div class="empty-state"><p>{{ t('actor.noResults') }}</p></div>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './actor-results.component.scss'
})
export class ActorResultsComponent implements OnInit, OnDestroy {
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  imageService = inject(ImageService);

  // --- Mode ---
  mode = signal<ActorMode>('popular');

  // --- Pagination ---
  currentPage = signal(1);
  totalPages = signal(0);
  totalResults = signal(0);

  // --- Search ---
  actorQuery = signal('');

  // --- Filters ---
  selectedGender = signal(0);
  selectedGenre = signal('');
  selectedCountry = signal('');
  selectedSort = signal('popularity');

  // --- Actor detail ---
  selectedActor = signal<Person | null>(null);
  actorMovies = signal<Movie[]>([]);

  // --- Loading ---
  loading = signal(false);
  batchLoading = signal(false);

  // --- Server-page mode data (popular/trending/search without filters) ---
  private serverPageActors = signal<Person[]>([]);

  // --- Filter mode data (batch loaded) ---
  private cachedActors = signal<Person[]>([]);
  private maxLoadedPage = signal(0);
  private maxAvailablePages = signal(500);

  // --- Computed: filtered + sorted actors (for filter mode) ---
  private allFilteredActors = computed(() => {
    const mode = this.mode();
    if (mode !== 'filter') return this.serverPageActors();
    return this.applyFilters(this.cachedActors());
  });

  // --- Computed: actors to display on current page ---
  displayedActors = computed(() => {
    const mode = this.mode();
    if (mode !== 'filter') {
      return this.allFilteredActors();
    }
    const page = this.currentPage();
    const start = (page - 1) * PAGE_SIZE;
    return this.allFilteredActors().slice(start, start + PAGE_SIZE);
  });

  // --- Computed: visible page numbers with ellipsis ---
  visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const pages: number[] = [1];
    if (current > 3) pages.push(-1);
    for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
      pages.push(i);
    }
    if (current < total - 2) pages.push(-1);
    if (pages[pages.length - 1] !== total) pages.push(total);
    return pages;
  });

  hasActiveFilters = computed(() =>
    this.selectedGender() !== 0 ||
    this.selectedGenre() !== '' ||
    this.selectedCountry() !== '' ||
    this.selectedSort() !== 'popularity'
  );

  modeTitle = computed(() => {
    switch (this.mode()) {
      case 'popular': return this.t('actor.popularTitle');
      case 'trending': return this.t('actor.trendingTitle');
      case 'search': return this.t('actor.searchResults');
      case 'filter': return this.t('actor.filterResults');
    }
  });

  // --- Internal ---
  private searchTimeout: any;
  private activeRequest?: Subscription;

  t(key: string): string { return this.ts.t(key); }

  ngOnInit(): void {
    this.loadServerPage(1);
  }

  ngOnDestroy(): void {
    clearTimeout(this.searchTimeout);
    this.activeRequest?.unsubscribe();
  }

  // ==================================================
  //  Server-side pagination (popular / trending / search)
  // ==================================================

  private getDataSource(): (page: number) => Observable<PersonSearchResponse> {
    const mode = this.mode();
    if (mode === 'search') {
      const query = this.actorQuery().trim();
      return (page) => this.movieService.searchPersons(query, page);
    }
    if (mode === 'trending') {
      return (page) => this.movieService.getTrendingActors(page);
    }
    return (page) => this.movieService.getPopularActors(page);
  }

  /** Loads 2 TMDB pages per display page to fill 36 actor slots. */
  private loadServerPage(page: number): void {
    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.currentPage.set(page);

    const source = this.getDataSource();
    const tmdbPage1 = (page - 1) * TMDB_PER_PAGE + 1;
    const tmdbPage2 = tmdbPage1 + 1;

    this.activeRequest = forkJoin([source(tmdbPage1), source(tmdbPage2)]).subscribe({
      next: ([res1, res2]) => {
        const all = [...res1.results, ...res2.results];
        const seen = new Set<number>();
        const actors = all.filter(p => {
          if (seen.has(p.id) || !p.profile_path || p.known_for_department !== 'Acting') return false;
          seen.add(p.id);
          return true;
        }).slice(0, PAGE_SIZE);

        this.serverPageActors.set(actors);
        const tmdbTotalPages = Math.min(res1.total_pages, 500);
        this.totalPages.set(Math.ceil(tmdbTotalPages / TMDB_PER_PAGE));
        this.totalResults.set(res1.total_results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // ==================================================
  //  Filter mode: batch loading + client-side filtering
  // ==================================================

  private loadFilterBatch(startPage: number): void {
    if (startPage > this.maxAvailablePages() || startPage > MAX_TMDB_PAGES) {
      this.batchLoading.set(false);
      this.loading.set(false);
      this.rebuildFilteredList();
      return;
    }
    if (this.batchLoading()) return;

    this.batchLoading.set(true);
    this.loading.set(true);

    const endPage = Math.min(startPage + BATCH_SIZE - 1, this.maxAvailablePages(), MAX_TMDB_PAGES);
    const requests: Observable<PersonSearchResponse>[] = [];
    for (let p = startPage; p <= endPage; p++) {
      requests.push(this.movieService.getPopularActors(p));
    }

    this.activeRequest?.unsubscribe();
    this.activeRequest = forkJoin(requests).subscribe({
      next: responses => {
        const existing = this.cachedActors();
        const seen = new Set(existing.map(a => a.id));
        const newActors: Person[] = [];

        for (const res of responses) {
          this.maxAvailablePages.set(Math.min(res.total_pages, 500));
          for (const actor of res.results) {
            if (!seen.has(actor.id) && actor.profile_path && actor.known_for_department === 'Acting') {
              seen.add(actor.id);
              newActors.push(actor);
            }
          }
        }

        this.cachedActors.set([...existing, ...newActors]);
        this.maxLoadedPage.set(endPage);
        this.rebuildFilteredList();

        this.batchLoading.set(false);
        this.loading.set(false);

        // Auto-load more if not enough results for current page
        const filtered = this.allFilteredActors();
        const neededForPage = this.currentPage() * PAGE_SIZE;
        if (filtered.length < neededForPage && endPage < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
          this.loadFilterBatch(endPage + 1);
        }
      },
      error: () => {
        this.batchLoading.set(false);
        this.loading.set(false);
      }
    });
  }

  private rebuildFilteredList(): void {
    const filtered = this.applyFilters(this.cachedActors());
    this.totalResults.set(filtered.length);
    this.totalPages.set(Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)));
    // Clamp currentPage
    if (this.currentPage() > this.totalPages()) {
      this.currentPage.set(Math.max(1, this.totalPages()));
    }
  }

  // ==================================================
  //  Filter logic
  // ==================================================

  private applyFilters(actors: Person[]): Person[] {
    let result = [...actors];

    const gender = this.selectedGender();
    if (gender !== 0) {
      result = result.filter(a => a.gender === gender);
    }

    const genre = this.selectedGenre();
    if (genre) {
      const genreId = GENRE_IDS[genre];
      if (genreId) {
        result = result.filter(a =>
          a.known_for?.some(m => m.genre_ids?.includes(genreId))
        );
      }
    }

    const country = this.selectedCountry();
    if (country) {
      result = result.filter(a =>
        a.known_for?.some(m => m.original_language === country)
      );
    }

    const sort = this.selectedSort();
    if (sort === 'nameAZ') {
      result.sort((a, b) => a.name.localeCompare(b.name));
    } else if (sort === 'nameZA') {
      result.sort((a, b) => b.name.localeCompare(a.name));
    } else {
      result.sort((a, b) => b.popularity - a.popularity);
    }

    return result;
  }

  onFilterChange(): void {
    this.currentPage.set(1);

    if (this.hasActiveFilters()) {
      if (this.mode() !== 'filter') {
        // Enter filter mode: start fresh batch loading
        this.mode.set('filter');
        this.cachedActors.set([]);
        this.maxLoadedPage.set(0);
        this.maxAvailablePages.set(500);
        this.loadFilterBatch(1);
      } else {
        // Already in filter mode: rebuild from existing cache
        this.rebuildFilteredList();
        // If too few results, load more
        const filtered = this.allFilteredActors();
        if (filtered.length < PAGE_SIZE && this.maxLoadedPage() < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
          this.loadFilterBatch(this.maxLoadedPage() + 1);
        }
      }
    } else {
      // No filters active → return to previous mode
      this.cachedActors.set([]);
      this.maxLoadedPage.set(0);
      if (this.actorQuery().trim().length >= 2) {
        this.mode.set('search');
        this.loadServerPage(1);
      } else {
        this.mode.set('popular');
        this.loadServerPage(1);
      }
    }
  }

  resetFilters(): void {
    this.selectedGender.set(0);
    this.selectedGenre.set('');
    this.selectedCountry.set('');
    this.selectedSort.set('popularity');
    this.onFilterChange();
  }

  // ==================================================
  //  Pagination navigation
  // ==================================================

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;

    if (this.mode() === 'filter') {
      // Client-side pagination
      const neededActors = page * PAGE_SIZE;
      const haveActors = this.allFilteredActors().length;

      this.currentPage.set(page);

      if (neededActors > haveActors && this.maxLoadedPage() < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
        this.loadFilterBatch(this.maxLoadedPage() + 1);
      }
    } else {
      // Server-side pagination
      this.loadServerPage(page);
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ==================================================
  //  Trending button
  // ==================================================

  showTrending(): void {
    this.actorQuery.set('');
    this.selectedActor.set(null);
    this.actorMovies.set([]);
    this.selectedGender.set(0);
    this.selectedGenre.set('');
    this.selectedCountry.set('');
    this.selectedSort.set('popularity');
    this.cachedActors.set([]);
    this.maxLoadedPage.set(0);
    this.mode.set('trending');
    this.loadServerPage(1);
  }

  // ==================================================
  //  Debounced text search
  // ==================================================

  onQueryInput(value: string): void {
    this.actorQuery.set(value);
    clearTimeout(this.searchTimeout);

    if (this.selectedActor()) {
      this.selectedActor.set(null);
      this.actorMovies.set([]);
    }

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.mode() === 'search') {
        if (this.hasActiveFilters()) {
          this.mode.set('filter');
          this.cachedActors.set([]);
          this.maxLoadedPage.set(0);
          this.currentPage.set(1);
          this.loadFilterBatch(1);
        } else {
          this.mode.set('popular');
          this.loadServerPage(1);
        }
      }
      return;
    }

    this.searchTimeout = setTimeout(() => this.executeSearch(trimmed), 400);
  }

  searchNow(): void {
    clearTimeout(this.searchTimeout);
    const trimmed = this.actorQuery().trim();
    if (trimmed.length >= 2) this.executeSearch(trimmed);
  }

  private executeSearch(query: string): void {
    this.activeRequest?.unsubscribe();
    this.mode.set('search');
    this.currentPage.set(1);
    this.cachedActors.set([]);
    this.maxLoadedPage.set(0);
    this.loadServerPage(1);
  }

  // ==================================================
  //  Actor selection
  // ==================================================

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
