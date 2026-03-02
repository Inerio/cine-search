import { Component, inject, OnInit, signal, computed, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { MovieService } from '../../services/movie.service';
import { TranslationService } from '../../services/translation.service';
import { Movie, Genre, Person } from '../../models/movie.model';
import { computeVisiblePages } from '../../utils/pagination';
import { SEARCH_DEBOUNCE_MS, DIRECTOR_SEARCH_DEBOUNCE_MS, DROPDOWN_HIDE_DELAY_MS, DROPDOWN_RESULTS_LIMIT, TV_RUNTIME } from '../../utils/constants';

type TvSearchMode = 'none' | 'text' | 'discover' | 'creator';

@Component({
  selector: 'app-tv-results',
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './tv-results.component.scss',
  template: `
    <div class="search-section">
      <div class="search-row">
        <input
          type="text"
          [ngModel]="tvQuery()"
          (ngModelChange)="onQueryInput($event)"
          (keyup.enter)="searchNow()"
          [placeholder]="t('tv.placeholder')"
          class="input search-input"
        />
        <button class="btn-trending" (click)="showTrending()" [title]="t('tv.trending')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          {{ t('tv.trending') }}
        </button>
      </div>

      <button class="filters-toggle" (click)="toggleFilters()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/>
          <line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/>
          <line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/>
          <line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/>
          <line x1="17" y1="16" x2="23" y2="16"/>
        </svg>
        {{ t('search.filters') }}
        @if (activeFilterCount() > 0) {
          <span class="filter-count">{{ activeFilterCount() }}</span>
        }
      </button>

      <div class="filters" [class.filters-collapsed]="!filtersOpen()">
        <select [ngModel]="selectedGenre()" (ngModelChange)="onFilterChange('genre', $event)" class="filter-select">
          <option [ngValue]="null">{{ t('filter.allGenres') }}</option>
          @for (genre of genres(); track $index) {
            <option [ngValue]="genre.id">{{ genre.name }}</option>
          }
        </select>

        <select [ngModel]="selectedDecade()" (ngModelChange)="onFilterChange('decade', $event)" class="filter-select">
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

        <select [ngModel]="selectedRating()" (ngModelChange)="onFilterChange('rating', $event)" class="filter-select">
          <option [ngValue]="null">{{ t('filter.minRating') }}</option>
          <option [ngValue]="6">6+</option>
          <option [ngValue]="7">7+</option>
          <option [ngValue]="8">8+</option>
          <option [ngValue]="9">9+</option>
        </select>

        <select [ngModel]="selectedSort()" (ngModelChange)="onFilterChange('sort', $event)" class="filter-select">
          <option [ngValue]="null">{{ t('filter.sortPopularity') }}</option>
          <option value="vote_average.desc">{{ t('filter.sortRating') }}</option>
          <option value="first_air_date.desc">{{ t('filter.sortRecent') }}</option>
        </select>

        <select [ngModel]="selectedLanguage()" (ngModelChange)="onFilterChange('language', $event)" class="filter-select">
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
          <option value="da">{{ t('filter.lang.da') }}</option>
          <option value="th">{{ t('filter.lang.th') }}</option>
          <option value="tr">{{ t('filter.lang.tr') }}</option>
        </select>

        <select [ngModel]="selectedRuntime()" (ngModelChange)="onFilterChange('runtime', $event)" class="filter-select">
          <option [ngValue]="null">{{ t('filter.allDurations') }}</option>
          <option value="short">{{ t('filter.tv.short') }}</option>
          <option value="medium">{{ t('filter.tv.medium') }}</option>
          <option value="long">{{ t('filter.tv.long') }}</option>
        </select>

        <div class="creator-autocomplete">
          <input
            type="text"
            [ngModel]="creatorQuery()"
            (ngModelChange)="onCreatorInput($event)"
            (focus)="onCreatorFocus()"
            (blur)="hideCreatorDropdown()"
            [placeholder]="selectedCreator() ? '' : t('filter.creatorPlaceholder')"
            class="input"
          />
          @if (selectedCreator()) {
            <span class="creator-tag">
              {{ selectedCreator()!.name }}
              <button class="tag-remove" (click)="clearCreatorAndApply()" type="button">&times;</button>
            </span>
          }
          @if (showCreatorDropdown() && creatorResults().length > 0) {
            <div class="dropdown-list">
              @for (person of creatorResults(); track person.id) {
                <div class="dropdown-item" (mousedown)="selectCreatorAndApply(person)">
                  <span class="dropdown-name">{{ person.name }}</span>
                  @if (person.known_for && person.known_for.length > 0) {
                    <span class="dropdown-hint">{{ person.known_for[0].title || person.known_for[0].name }}</span>
                  }
                </div>
              }
            </div>
          }
        </div>

        @if (searched()) {
          <button class="btn-reset" (click)="resetFilters()">{{ t('search.reset') }}</button>
        }
      </div>

      <!-- Results area -->
      <div class="results-area" [class.is-loading]="loading()">

        @if (hasResults()) {
          <div class="results-count">{{ totalResults() }} {{ t('search.resultsCount') }}</div>
          <div class="movie-grid">
            @for (show of tvResults(); track show.id) {
              <app-movie-card [movie]="show" linkPrefix="/tv" />
            }
          </div>

          @if (totalPages() > 1) {
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
              <input type="number" class="page-jump-input"
                     [min]="1" [max]="totalPages()"
                     [placeholder]="currentPage() + '/' + totalPages()"
                     (keyup.enter)="onPageJump($event)" />
            </div>
          }
        }

        @if (!hasResults() && !searched() && defaultShows().length > 0) {
          <div class="default-section">
            <div class="results-count">{{ trendingTotalResults() }} {{ t('search.resultsCount') }}</div>
            <h3 class="default-title">{{ t('tv.trendingTitle') }}</h3>
            <div class="movie-grid">
              @for (show of defaultShows(); track show.id) {
                <app-movie-card [movie]="show" linkPrefix="/tv" />
              }
            </div>

            @if (trendingTotalPages() > 1) {
              <div class="pagination">
                <button class="page-btn" [disabled]="trendingPage() <= 1" (click)="goToTrendingPage(trendingPage() - 1)">
                  &#8592; {{ t('search.previous') }}
                </button>
                @for (p of trendingVisiblePages(); track $index) {
                  @if (p === -1) {
                    <span class="page-ellipsis">...</span>
                  } @else {
                    <button class="page-num" [class.active]="p === trendingPage()" (click)="goToTrendingPage(p)">{{ p }}</button>
                  }
                }
                <button class="page-btn" [disabled]="trendingPage() >= trendingTotalPages()" (click)="goToTrendingPage(trendingPage() + 1)">
                  {{ t('search.next') }} &#8594;
                </button>
                <input type="number" class="page-jump-input"
                       [min]="1" [max]="trendingTotalPages()"
                       [placeholder]="trendingPage() + '/' + trendingTotalPages()"
                       (keyup.enter)="onTrendingPageJump($event)" />
              </div>
            }
          </div>
        }

        @if (loading() && !hasResults() && defaultShows().length === 0) {
          <div class="loader"><div class="spinner"></div></div>
        }

        @if (!loading() && searched() && !hasResults()) {
          <div class="empty-state"><p>{{ t('tv.noResults') }}</p></div>
        }
      </div>
    </div>
  `
})
export class TvResultsComponent implements OnInit {
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  private destroyRef = inject(DestroyRef);

  // --- UI state ---
  tvQuery = signal('');
  tvResults = signal<Movie[]>([]);
  genres = signal<Genre[]>([]);
  loading = signal(false);
  searched = signal(false);
  totalResults = signal(0);
  currentPage = signal(1);
  totalPages = signal(0);
  searchMode = signal<TvSearchMode>('none');
  defaultShows = signal<Movie[]>([]);
  trendingPage = signal(1);
  trendingTotalPages = signal(0);
  trendingTotalResults = signal(0);

  // --- Filters ---
  selectedGenre = signal<number | null>(null);
  selectedRating = signal<number | null>(null);
  selectedSort = signal<string | null>(null);
  selectedLanguage = signal<string | null>(null);
  selectedRuntime = signal<string | null>(null);
  selectedDecade = signal<string | null>(null);

  // --- Creator autocomplete ---
  creatorQuery = signal('');
  creatorResults = signal<Person[]>([]);
  selectedCreator = signal<Person | null>(null);
  showCreatorDropdown = signal(false);
  private allCreatorShows: Movie[] = [];

  // --- Mobile ---
  filtersOpen = signal(false);

  // --- Computed ---
  hasResults = computed(() => this.tvResults().length > 0);
  visiblePages = computed(() => computeVisiblePages(this.totalPages(), this.currentPage()));
  trendingVisiblePages = computed(() => computeVisiblePages(this.trendingTotalPages(), this.trendingPage()));
  activeFilterCount = computed(() => {
    let count = 0;
    if (this.selectedGenre()) count++;
    if (this.selectedDecade()) count++;
    if (this.selectedRating()) count++;
    if (this.selectedSort()) count++;
    if (this.selectedLanguage()) count++;
    if (this.selectedRuntime()) count++;
    if (this.selectedCreator()) count++;
    return count;
  });

  // --- Internal ---
  private textSearchTimeout: ReturnType<typeof setTimeout> | undefined;
  private creatorSearchTimeout: ReturnType<typeof setTimeout> | undefined;
  private activeRequest?: Subscription;

  private static readonly PAGE_SIZE = 20;

  t(key: string): string { return this.ts.t(key); }

  toggleFilters(): void {
    this.filtersOpen.update(v => !v);
  }

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.textSearchTimeout);
      clearTimeout(this.creatorSearchTimeout);
      this.activeRequest?.unsubscribe();
    });

    this.movieService.getTvGenres().subscribe(res => {
      // TMDB combines several TV genres with " & " (e.g. "Sci-Fi & Fantasy",
      // "Action & Adventure", "War & Politics"). Split them into separate
      // entries so the dropdown stays compact and matches the Film tab.
      const expanded: Genre[] = [];
      for (const g of res.genres) {
        const parts = g.name.split(' & ');
        if (parts.length === 2) {
          expanded.push({ id: g.id, name: parts[0] });
          expanded.push({ id: g.id, name: parts[1] });
        } else {
          expanded.push(g);
        }
      }
      this.genres.set(expanded);
    });
    this.loadTrendingTv();
  }

  // =====================
  //  Default content
  // =====================

  private loadTrendingTv(page = 1): void {
    this.loading.set(true);
    this.trendingPage.set(page);
    this.movieService.getTrendingTv(page).subscribe({
      next: res => {
        this.defaultShows.set(res.results);
        this.trendingTotalPages.set(Math.min(res.total_pages, 500));
        this.trendingTotalResults.set(res.total_results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // =====================
  //  Debounced text search
  // =====================

  onQueryInput(value: string): void {
    this.tvQuery.set(value);
    clearTimeout(this.textSearchTimeout);

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.searchMode() === 'text' || !this.searched()) {
        this.searched.set(false);
        this.tvResults.set([]);
        this.totalResults.set(0);
        this.currentPage.set(1);
        this.totalPages.set(0);
        this.searchMode.set('none');
        if (this.defaultShows().length === 0) this.loadTrendingTv();
      }
      return;
    }

    this.textSearchTimeout = setTimeout(() => this.executeTextSearch(1), SEARCH_DEBOUNCE_MS);
  }

  searchNow(): void {
    clearTimeout(this.textSearchTimeout);
    const trimmed = this.tvQuery().trim();
    if (trimmed.length >= 2) this.executeTextSearch(1);
  }

  private executeTextSearch(page: number): void {
    const query = this.tvQuery().trim();
    if (!query) return;

    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.searched.set(true);
    this.searchMode.set('text');
    this.currentPage.set(page);

    this.activeRequest = this.movieService.searchTv(query, page).subscribe({
      next: res => {
        this.tvResults.set(res.results);
        this.totalResults.set(res.total_results);
        this.totalPages.set(Math.min(res.total_pages, 500));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // =====================
  //  Filters (auto-apply)
  // =====================

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onFilterChange(filter: string, value: any): void {
    switch (filter) {
      case 'genre': this.selectedGenre.set(value); break;
      case 'decade': this.selectedDecade.set(value); break;
      case 'rating': this.selectedRating.set(value); break;
      case 'sort': this.selectedSort.set(value); break;
      case 'language': this.selectedLanguage.set(value); break;
      case 'runtime': this.selectedRuntime.set(value); break;
    }
    this.currentPage.set(1);

    if (this.selectedCreator()) {
      this.applyCreatorFilters(1);
    } else {
      this.executeDiscover(1);
    }
  }

  private buildDiscoverParams(page: number) {
    const runtime = this.selectedRuntime();
    let runtimeGte: number | undefined;
    let runtimeLte: number | undefined;
    if (runtime === 'short') runtimeLte = TV_RUNTIME.SHORT_MAX;
    else if (runtime === 'medium') { runtimeGte = TV_RUNTIME.MEDIUM_MIN; runtimeLte = TV_RUNTIME.MEDIUM_MAX; }
    else if (runtime === 'long') runtimeGte = TV_RUNTIME.LONG_MIN;

    const decade = this.selectedDecade();
    let decadeStart: string | undefined;
    let decadeEnd: string | undefined;
    if (decade) {
      const y = parseInt(decade, 10);
      if (y === 1900) { decadeEnd = '1959-12-31'; }
      else { decadeStart = `${y}-01-01`; decadeEnd = `${y + 9}-12-31`; }
    }

    return {
      genreId: this.selectedGenre() ?? undefined,
      minRating: this.selectedRating() ?? undefined,
      language: this.selectedLanguage() ?? undefined,
      sortBy: this.selectedSort() ?? undefined,
      runtimeGte,
      runtimeLte,
      decadeStart,
      decadeEnd,
      page,
    };
  }

  private executeDiscover(page: number): void {
    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.searched.set(true);
    this.searchMode.set('discover');
    this.currentPage.set(page);

    this.activeRequest = this.movieService.discoverTv(this.buildDiscoverParams(page)).subscribe({
      next: res => {
        this.tvResults.set(res.results);
        this.totalResults.set(res.total_results);
        this.totalPages.set(Math.min(res.total_pages, 500));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // =====================
  //  Creator autocomplete
  // =====================

  onCreatorInput(value: string): void {
    this.creatorQuery.set(value);
    if (this.selectedCreator()) this.selectedCreator.set(null);

    clearTimeout(this.creatorSearchTimeout);
    if (value.trim().length >= 2) {
      this.creatorSearchTimeout = setTimeout(() => {
        this.movieService.searchPersons(value.trim()).subscribe(res => {
          const creators = res.results.slice(0, DROPDOWN_RESULTS_LIMIT);
          this.creatorResults.set(creators);
          this.showCreatorDropdown.set(creators.length > 0);
        });
      }, DIRECTOR_SEARCH_DEBOUNCE_MS);
    } else {
      this.creatorResults.set([]);
      this.showCreatorDropdown.set(false);
    }
  }

  onCreatorFocus(): void {
    if (this.creatorResults().length > 0 && !this.selectedCreator()) {
      this.showCreatorDropdown.set(true);
    }
  }

  selectCreatorAndApply(person: Person): void {
    this.selectedCreator.set(person);
    this.creatorQuery.set(person.name);
    this.showCreatorDropdown.set(false);
    this.creatorResults.set([]);
    this.currentPage.set(1);
    this.executeCreatorSearch();
  }

  clearCreatorAndApply(): void {
    this.clearCreator();
    this.currentPage.set(1);
    if (this.searched()) {
      this.executeDiscover(1);
    }
  }

  private clearCreator(): void {
    this.selectedCreator.set(null);
    this.creatorQuery.set('');
    this.creatorResults.set([]);
    this.showCreatorDropdown.set(false);
    this.allCreatorShows = [];
  }

  hideCreatorDropdown(): void {
    setTimeout(() => this.showCreatorDropdown.set(false), DROPDOWN_HIDE_DELAY_MS);
  }

  private executeCreatorSearch(): void {
    const creator = this.selectedCreator();
    if (!creator) return;

    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.searched.set(true);
    this.searchMode.set('creator');

    this.activeRequest = this.movieService.getPersonTvShows(creator.id).subscribe({
      next: res => {
        // Merge cast + crew, deduplicate by id
        const all = [...(res.cast || []), ...(res.crew || [])];
        const seen = new Set<number>();
        this.allCreatorShows = all.filter(s => {
          if (seen.has(s.id)) return false;
          seen.add(s.id);
          return true;
        });
        this.applyCreatorFilters(1);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private applyCreatorFilters(page: number): void {
    let filtered = [...this.allCreatorShows];

    // Genre filter
    const genre = this.selectedGenre();
    if (genre) filtered = filtered.filter(s => s.genre_ids?.includes(genre));

    // Decade filter
    const decade = this.selectedDecade();
    if (decade) {
      const y = parseInt(decade, 10);
      if (y === 1900) {
        filtered = filtered.filter(s => {
          const date = s.first_air_date || s.release_date || '';
          const year = parseInt(date.substring(0, 4), 10);
          return year > 0 && year < 1960;
        });
      } else {
        filtered = filtered.filter(s => {
          const date = s.first_air_date || s.release_date || '';
          const year = parseInt(date.substring(0, 4), 10);
          return year >= y && year <= y + 9;
        });
      }
    }

    // Rating filter
    const rating = this.selectedRating();
    if (rating) filtered = filtered.filter(s => s.vote_average >= rating);

    // Language filter
    const lang = this.selectedLanguage();
    if (lang) filtered = filtered.filter(s => s.original_language === lang);

    // Sort
    const sort = this.selectedSort();
    if (sort === 'vote_average.desc') {
      filtered.sort((a, b) => b.vote_average - a.vote_average);
    } else if (sort === 'first_air_date.desc') {
      filtered.sort((a, b) => {
        const dateA = a.first_air_date || a.release_date || '';
        const dateB = b.first_air_date || b.release_date || '';
        return dateB.localeCompare(dateA);
      });
    } else {
      filtered.sort((a, b) => b.popularity - a.popularity);
    }

    // Client-side pagination
    const total = filtered.length;
    const pages = Math.ceil(total / TvResultsComponent.PAGE_SIZE) || 1;
    const safePage = Math.min(page, pages);
    const start = (safePage - 1) * TvResultsComponent.PAGE_SIZE;
    const pageResults = filtered.slice(start, start + TvResultsComponent.PAGE_SIZE);

    this.tvResults.set(pageResults);
    this.totalResults.set(total);
    this.totalPages.set(pages);
    this.currentPage.set(safePage);
    this.searched.set(true);
    this.searchMode.set('creator');
  }

  // =====================
  //  Pagination
  // =====================

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (this.searchMode() === 'text') this.executeTextSearch(page);
    else if (this.searchMode() === 'creator') this.applyCreatorFilters(page);
    else this.executeDiscover(page);
  }

  onPageJump(event: Event): void {
    const input = event.target as HTMLInputElement;
    const page = parseInt(input.value, 10);
    if (page >= 1 && page <= this.totalPages()) {
      this.goToPage(page);
    }
    input.value = '';
  }

  goToTrendingPage(page: number): void {
    if (page < 1 || page > this.trendingTotalPages() || page === this.trendingPage()) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.loadTrendingTv(page);
  }

  onTrendingPageJump(event: Event): void {
    const input = event.target as HTMLInputElement;
    const page = parseInt(input.value, 10);
    if (page >= 1 && page <= this.trendingTotalPages()) {
      this.goToTrendingPage(page);
    }
    input.value = '';
  }

  // =====================
  //  Trending / Reset
  // =====================

  showTrending(): void {
    this.resetFilters();
  }

  resetFilters(): void {
    this.selectedGenre.set(null);
    this.selectedRating.set(null);
    this.selectedSort.set(null);
    this.selectedLanguage.set(null);
    this.selectedRuntime.set(null);
    this.selectedDecade.set(null);
    this.clearCreator();
    this.tvQuery.set('');
    this.searched.set(false);
    this.searchMode.set('none');
    this.tvResults.set([]);
    this.totalResults.set(0);
    this.currentPage.set(1);
    this.totalPages.set(0);
    this.trendingPage.set(1);
    this.loadTrendingTv();
  }
}
