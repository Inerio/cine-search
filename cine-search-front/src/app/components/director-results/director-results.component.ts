import { Component, inject, OnInit, signal, computed, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, forkJoin, Subscription } from 'rxjs';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { ImageService } from '../../services/image.service';
import { TranslationService } from '../../services/translation.service';
import { Movie, Genre, Person, PersonSearchResponse } from '../../models/movie.model';
import { computeVisiblePages } from '../../utils/pagination';
import { applyPersonFilters } from '../../utils/person-filters';

/**
 * Directors are sparse in TMDB popular/trending results (~10-15% of persons).
 * Unlike actors, ALL modes use batch-loading + client-side pagination
 * to guarantee we always fill 36 slots per page.
 */
type DirectorMode = 'popular' | 'trending' | 'search' | 'filter';

const PAGE_SIZE = 36;           // 9 columns × 4 rows
const BATCH_SIZE = 10;          // Load 10 TMDB pages per batch (200 persons)
const MAX_TMDB_PAGES = 500;     // Higher cap for directors since they are sparse

@Component({
  selector: 'app-director-results',
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="director-search">
      <div class="search-row">
        <input
          type="text"
          [ngModel]="directorQuery()"
          (ngModelChange)="onQueryInput($event)"
          (keyup.enter)="searchNow()"
          [placeholder]="t('director.placeholder')"
          class="input search-input"
        />
        <button class="btn-trending" (click)="showTrending()" [title]="t('director.trending')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          {{ t('director.trending') }}
        </button>
      </div>

      @if (!selectedDirector()) {
        <!-- Person filters -->
        <div class="filters">
          <select class="filter-select" [ngModel]="selectedGender()" (ngModelChange)="selectedGender.set(+$event); onFilterChange()">
            <option [ngValue]="0">{{ t('actor.filter.allGenders') }}</option>
            <option [ngValue]="2">{{ t('actor.filter.male') }}</option>
            <option [ngValue]="1">{{ t('actor.filter.female') }}</option>
            <option [ngValue]="3">{{ t('actor.filter.nonBinary') }}</option>
          </select>

          <select class="filter-select" [ngModel]="selectedGenre()" (ngModelChange)="selectedGenre.set($event); onFilterChange()">
            <option value="">{{ t('director.filter.allGenres') }}</option>
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
            <option value="">{{ t('director.filter.allCountries') }}</option>
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
            <option value="popularity">{{ t('director.filter.sortPopularity') }}</option>
            <option value="nameAZ">{{ t('director.filter.sortNameAZ') }}</option>
            <option value="nameZA">{{ t('director.filter.sortNameZA') }}</option>
          </select>

          @if (hasActiveFilters()) {
            <button class="btn-reset" (click)="resetFilters()">{{ t('director.reset') }}</button>
          }
        </div>
      }

      @if (selectedDirector()) {
        <!-- Filmography filters -->
        <div class="filters">
          <select [ngModel]="filmGenre()" (ngModelChange)="filmGenre.set($event)" class="filter-select">
            <option [ngValue]="null">{{ t('filter.allGenres') }}</option>
            @for (genre of genres(); track genre.id) {
              <option [ngValue]="genre.id">{{ genre.name }}</option>
            }
          </select>

          <select [ngModel]="filmDecade()" (ngModelChange)="filmDecade.set($event)" class="filter-select">
            <option [ngValue]="null">{{ t('filter.allPeriods') }}</option>
            <option value="2020">2020s</option>
            <option value="2010">2010s</option>
            <option value="2000">2000s</option>
            <option value="1990">1990s</option>
            <option value="1980">1980s</option>
            <option value="1970">1970s</option>
            <option value="1960">1960s</option>
            <option value="1900">{{ t('filter.before1960') }}</option>
          </select>

          <select [ngModel]="filmRating()" (ngModelChange)="filmRating.set($event)" class="filter-select">
            <option [ngValue]="null">{{ t('filter.minRating') }}</option>
            <option [ngValue]="6">6+</option>
            <option [ngValue]="7">7+</option>
            <option [ngValue]="8">8+</option>
            <option [ngValue]="9">9+</option>
          </select>

          <select [ngModel]="filmSort()" (ngModelChange)="filmSort.set($event)" class="filter-select">
            <option value="popularity">{{ t('filter.sortPopularity') }}</option>
            <option value="vote_average">{{ t('filter.sortRating') }}</option>
            <option value="recent">{{ t('filter.sortRecent') }}</option>
          </select>

          <select [ngModel]="filmLanguage()" (ngModelChange)="filmLanguage.set($event)" class="filter-select">
            <option [ngValue]="null">{{ t('filter.allLanguages') }}</option>
            <option value="fr">{{ t('filter.lang.fr') }}</option>
            <option value="en">{{ t('filter.lang.en') }}</option>
            <option value="ja">{{ t('filter.lang.ja') }}</option>
            <option value="ko">{{ t('filter.lang.ko') }}</option>
            <option value="es">{{ t('filter.lang.es') }}</option>
            <option value="de">{{ t('filter.lang.de') }}</option>
            <option value="it">{{ t('filter.lang.it') }}</option>
            <option value="pt">{{ t('filter.lang.pt') }}</option>
            <option value="hi">{{ t('filter.lang.hi') }}</option>
            <option value="zh">{{ t('filter.lang.zh') }}</option>
            <option value="ru">{{ t('filter.lang.ru') }}</option>
            <option value="sv">{{ t('filter.lang.sv') }}</option>
            <option value="tr">{{ t('filter.lang.tr') }}</option>
          </select>

          @if (hasActiveFilmFilters()) {
            <button class="btn-reset" (click)="resetFilmFilters()">{{ t('search.reset') }}</button>
          }
        </div>
      }

      <!-- Results area -->
      <div class="results-area" [class.is-loading]="loading() && displayedDirectors().length > 0">

        @if (selectedDirector()) {
          <div class="selected-person">
            <button class="back-btn" (click)="clearSelection()">&#8592; {{ t('director.back') }}</button>
            <div class="person-header">
              <img
                [src]="imageService.getProfileUrl(selectedDirector()!.profile_path, 'w342')"
                [alt]="selectedDirector()!.name"
                class="person-photo-large"
              />
              <div>
                <h2>{{ selectedDirector()!.name }}</h2>
                <p class="filmography-count">{{ filteredFilmography().length }} {{ t('director.films') }}</p>
              </div>
            </div>
            <div class="movie-grid">
              @for (movie of filteredFilmography(); track movie.id) {
                <app-movie-card [movie]="movie" />
              }
            </div>
          </div>
        }

        @if (!selectedDirector()) {
          @if (displayedDirectors().length > 0 || batchLoading()) {
            <div class="results-info">
              <h3 class="default-title">{{ modeTitle() }}</h3>
              @if (totalResults() > 0) {
                <span class="results-count">{{ totalResults() }} {{ t('director.resultsCount') }}</span>
              }
            </div>
          }

          @if (displayedDirectors().length > 0) {
            <div class="person-grid">
              @for (director of displayedDirectors(); track director.id) {
                <div class="person-card" (click)="selectDirector(director)">
                  <img
                    [src]="imageService.getProfileUrl(director.profile_path)"
                    [alt]="director.name"
                    class="person-photo"
                  />
                  <div class="person-info">
                    <h3>{{ director.name }}</h3>
                    <span class="department">{{ director.known_for_department }}</span>
                  </div>
                </div>
              }
            </div>
          }

          @if (batchLoading() && displayedDirectors().length > 0) {
            <div class="batch-loading-hint">{{ t('director.loadingMore') }}</div>
          }

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

          @if (loading() && displayedDirectors().length === 0 && !batchLoading()) {
            <div class="loader"><div class="spinner"></div></div>
          }

          @if (!loading() && !batchLoading() && displayedDirectors().length === 0) {
            <div class="empty-state"><p>{{ t('director.noResults') }}</p></div>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './director-results.component.scss'
})
export class DirectorResultsComponent implements OnInit {
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  imageService = inject(ImageService);

  // --- Mode ---
  mode = signal<DirectorMode>('popular');

  // --- Pagination (always client-side for directors) ---
  currentPage = signal(1);
  totalPages = signal(0);
  totalResults = signal(0);

  // --- Search ---
  directorQuery = signal('');

  // --- Filters ---
  selectedGender = signal(0);
  selectedGenre = signal('');
  selectedCountry = signal('');
  selectedSort = signal('popularity');

  // --- Director detail ---
  selectedDirector = signal<Person | null>(null);
  directorMovies = signal<Movie[]>([]);

  // --- Filmography filters (when person selected) ---
  genres = signal<Genre[]>([]);
  filmGenre = signal<number | null>(null);
  filmDecade = signal<string | null>(null);
  filmRating = signal<number | null>(null);
  filmSort = signal<string>('popularity');
  filmLanguage = signal<string | null>(null);

  // --- Loading ---
  loading = signal(false);
  batchLoading = signal(false);

  // --- All modes use batch-loaded data (directors are sparse in TMDB results) ---
  private cachedDirectors = signal<Person[]>([]);
  private maxLoadedPage = signal(0);
  private maxAvailablePages = signal(500);

  // --- Computed: filtered + sorted directors ---
  allFilteredDirectors = computed(() => {
    return applyPersonFilters(this.cachedDirectors(), {
      gender: this.selectedGender(),
      genre: this.selectedGenre(),
      country: this.selectedCountry(),
      sort: this.selectedSort()
    });
  });

  // --- Computed: directors to display on current page ---
  displayedDirectors = computed(() => {
    const start = (this.currentPage() - 1) * PAGE_SIZE;
    return this.allFilteredDirectors().slice(start, start + PAGE_SIZE);
  });

  visiblePages = computed(() => computeVisiblePages(this.totalPages(), this.currentPage()));

  hasActiveFilters = computed(() =>
    this.selectedGender() !== 0 ||
    this.selectedGenre() !== '' ||
    this.selectedCountry() !== '' ||
    this.selectedSort() !== 'popularity'
  );

  filteredFilmography = computed(() => {
    let movies = this.directorMovies();
    const genre = this.filmGenre();
    if (genre) movies = movies.filter(m => m.genre_ids?.includes(genre));
    const decade = this.filmDecade();
    if (decade) {
      const y = parseInt(decade, 10);
      if (y === 1900) {
        movies = movies.filter(m => { const yr = parseInt(m.release_date?.substring(0, 4), 10); return yr > 0 && yr < 1960; });
      } else {
        movies = movies.filter(m => { const yr = parseInt(m.release_date?.substring(0, 4), 10); return yr >= y && yr <= y + 9; });
      }
    }
    const rating = this.filmRating();
    if (rating) movies = movies.filter(m => m.vote_average >= rating);
    const lang = this.filmLanguage();
    if (lang) movies = movies.filter(m => m.original_language === lang);
    const sort = this.filmSort();
    return [...movies].sort((a, b) => {
      if (sort === 'vote_average') return b.vote_average - a.vote_average;
      if (sort === 'recent') return (b.release_date || '').localeCompare(a.release_date || '');
      return b.popularity - a.popularity;
    });
  });

  hasActiveFilmFilters = computed(() =>
    this.filmGenre() !== null ||
    this.filmDecade() !== null ||
    this.filmRating() !== null ||
    this.filmSort() !== 'popularity' ||
    this.filmLanguage() !== null
  );

  modeTitle = computed(() => {
    switch (this.mode()) {
      case 'popular': return this.t('director.popularTitle');
      case 'trending': return this.t('director.trendingTitle');
      case 'search': return this.t('director.searchResults');
      case 'filter': return this.t('director.filterResults');
    }
  });

  private searchTimeout: any;
  private activeRequest?: Subscription;

  t(key: string): string { return this.ts.t(key); }

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.searchTimeout);
      this.activeRequest?.unsubscribe();
    });

    this.movieService.getGenres().subscribe(res => this.genres.set(res.genres));

    const personId = this.route.snapshot.queryParamMap.get('personId');
    if (personId && this.route.snapshot.queryParamMap.get('tab') === 'director') {
      this.loading.set(true);
      this.movieService.getPersonDetails(+personId).subscribe({
        next: person => this.selectDirector(person),
        error: () => {
          this.loading.set(false);
          this.loadBatch(1);
        }
      });
    } else {
      this.loadBatch(1);
    }
  }

  // ==================================================
  //  Data source based on current mode
  // ==================================================

  private getDataSource(): (page: number) => Observable<PersonSearchResponse> {
    const mode = this.mode();
    if (mode === 'search') {
      const query = this.directorQuery().trim();
      return (page) => this.movieService.searchPersons(query, page);
    }
    if (mode === 'trending') {
      return (page) => this.movieService.getTrendingActors(page);
    }
    return (page) => this.movieService.getPopularActors(page);
  }

  // ==================================================
  //  Batch loading — used for ALL modes
  // ==================================================

  private loadBatch(startPage: number): void {
    if (startPage > this.maxAvailablePages() || startPage > MAX_TMDB_PAGES) {
      this.batchLoading.set(false);
      this.loading.set(false);
      this.rebuildList();
      return;
    }
    if (this.batchLoading()) return;

    this.batchLoading.set(true);
    if (this.cachedDirectors().length === 0) {
      this.loading.set(true);
    }

    const source = this.getDataSource();
    const endPage = Math.min(startPage + BATCH_SIZE - 1, this.maxAvailablePages(), MAX_TMDB_PAGES);
    const requests: Observable<PersonSearchResponse>[] = [];
    for (let p = startPage; p <= endPage; p++) {
      requests.push(source(p));
    }

    this.activeRequest?.unsubscribe();
    this.activeRequest = forkJoin(requests).subscribe({
      next: responses => {
        const existing = this.cachedDirectors();
        const seen = new Set(existing.map(d => d.id));
        const newDirectors: Person[] = [];

        for (const res of responses) {
          this.maxAvailablePages.set(Math.min(res.total_pages, 500));
          for (const person of res.results) {
            if (!seen.has(person.id) && person.profile_path && person.known_for_department === 'Directing') {
              seen.add(person.id);
              newDirectors.push(person);
            }
          }
        }

        this.cachedDirectors.set([...existing, ...newDirectors]);
        this.maxLoadedPage.set(endPage);
        this.rebuildList();

        this.batchLoading.set(false);
        this.loading.set(false);

        const filtered = this.allFilteredDirectors();
        const neededForPage = this.currentPage() * PAGE_SIZE;
        if (filtered.length < neededForPage && endPage < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
          this.loadBatch(endPage + 1);
        }
      },
      error: () => {
        this.batchLoading.set(false);
        this.loading.set(false);
      }
    });
  }

  private rebuildList(): void {
    const filtered = this.allFilteredDirectors();
    this.totalResults.set(filtered.length);
    this.totalPages.set(Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)));
    if (this.currentPage() > this.totalPages()) {
      this.currentPage.set(Math.max(1, this.totalPages()));
    }
  }

  // ==================================================
  //  Filter change handler
  // ==================================================

  onFilterChange(): void {
    this.currentPage.set(1);

    if (this.hasActiveFilters()) {
      if (this.mode() !== 'filter') {
        const previousMode = this.mode();
        this.mode.set('filter');
        if (this.cachedDirectors().length > 0 && (previousMode === 'popular' || previousMode === 'trending')) {
          this.rebuildList();
          const filtered = this.allFilteredDirectors();
          if (filtered.length < PAGE_SIZE && this.maxLoadedPage() < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
            this.loadBatch(this.maxLoadedPage() + 1);
          }
        } else {
          this.cachedDirectors.set([]);
          this.maxLoadedPage.set(0);
          this.maxAvailablePages.set(500);
          this.loadBatch(1);
        }
      } else {
        this.rebuildList();
        const filtered = this.allFilteredDirectors();
        if (filtered.length < PAGE_SIZE && this.maxLoadedPage() < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
          this.loadBatch(this.maxLoadedPage() + 1);
        }
      }
    } else {
      if (this.directorQuery().trim().length >= 2) {
        this.mode.set('search');
      } else {
        this.mode.set('popular');
      }
      this.rebuildList();
    }
  }

  resetFilters(): void {
    this.selectedGender.set(0);
    this.selectedGenre.set('');
    this.selectedCountry.set('');
    this.selectedSort.set('popularity');
    this.onFilterChange();
  }

  resetFilmFilters(): void {
    this.filmGenre.set(null);
    this.filmDecade.set(null);
    this.filmRating.set(null);
    this.filmSort.set('popularity');
    this.filmLanguage.set(null);
  }

  // ==================================================
  //  Pagination — always client-side
  // ==================================================

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;

    const neededDirectors = page * PAGE_SIZE;
    const haveDirectors = this.allFilteredDirectors().length;
    this.currentPage.set(page);

    if (neededDirectors > haveDirectors && this.maxLoadedPage() < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)) {
      this.loadBatch(this.maxLoadedPage() + 1);
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  showTrending(): void {
    this.directorQuery.set('');
    this.selectedDirector.set(null);
    this.directorMovies.set([]);
    this.selectedGender.set(0);
    this.selectedGenre.set('');
    this.selectedCountry.set('');
    this.selectedSort.set('popularity');
    this.cachedDirectors.set([]);
    this.maxLoadedPage.set(0);
    this.maxAvailablePages.set(500);
    this.mode.set('trending');
    this.loadBatch(1);
  }

  onQueryInput(value: string): void {
    this.directorQuery.set(value);
    clearTimeout(this.searchTimeout);

    if (this.selectedDirector()) {
      this.selectedDirector.set(null);
      this.directorMovies.set([]);
    }

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.mode() === 'search') {
        this.mode.set('popular');
        this.cachedDirectors.set([]);
        this.maxLoadedPage.set(0);
        this.maxAvailablePages.set(500);
        this.currentPage.set(1);
        this.loadBatch(1);
      }
      return;
    }

    this.searchTimeout = setTimeout(() => this.executeSearch(), 400);
  }

  searchNow(): void {
    clearTimeout(this.searchTimeout);
    if (this.directorQuery().trim().length >= 2) this.executeSearch();
  }

  private executeSearch(): void {
    this.activeRequest?.unsubscribe();
    this.mode.set('search');
    this.currentPage.set(1);
    this.cachedDirectors.set([]);
    this.maxLoadedPage.set(0);
    this.maxAvailablePages.set(500);
    this.loadBatch(1);
  }

  // ==================================================
  //  Director selection — filmography uses CREW
  // ==================================================

  selectDirector(director: Person): void {
    this.selectedDirector.set(director);
    this.loading.set(true);

    // Persist personId in URL so back-navigation restores the director
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { personId: director.id },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });

    this.activeRequest?.unsubscribe();
    this.activeRequest = this.movieService.getPersonMovies(director.id).subscribe({
      next: res => {
        const directedMovies = (res.crew || [])
          .filter(m => m.job === 'Director' && m.poster_path)
          .sort((a, b) => b.popularity - a.popularity);

        const seen = new Set<number>();
        const unique = directedMovies.filter(m => {
          if (seen.has(m.id)) return false;
          seen.add(m.id);
          return true;
        });

        this.directorMovies.set(unique);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  clearSelection(): void {
    this.selectedDirector.set(null);
    this.directorMovies.set([]);
    this.resetFilmFilters();

    // Remove personId from URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { personId: null },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
