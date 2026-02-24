import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MovieCardComponent } from '../movie-card/movie-card.component';
import { SceneSearchComponent } from '../scene-search/scene-search.component';
import { ActorResultsComponent } from '../actor-results/actor-results.component';
import { MovieService } from '../../services/movie.service';
import { TranslationService } from '../../services/translation.service';
import { Movie, Genre, Person } from '../../models/movie.model';

type SearchTab = 'movie' | 'actor' | 'scene';
type SearchMode = 'none' | 'text' | 'discover';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, MovieCardComponent, SceneSearchComponent, ActorResultsComponent],
  template: `
    <div class="search-page">
      <div class="tabs">
        <button class="tab" [class.active]="activeTab() === 'movie'" (click)="setTab('movie')">{{ t('search.tab.movie') }}</button>
        <button class="tab" [class.active]="activeTab() === 'actor'" (click)="setTab('actor')">{{ t('search.tab.actor') }}</button>
        <button class="tab" [class.active]="activeTab() === 'scene'" (click)="setTab('scene')">{{ t('search.tab.advanced') }}</button>
      </div>

      @if (activeTab() === 'movie') {
        <div class="search-section">
          <div class="search-row">
            <input
              type="text"
              [ngModel]="movieQuery()"
              (ngModelChange)="onQueryInput($event)"
              (keyup.enter)="searchNow()"
              [placeholder]="t('search.placeholder')"
              class="input search-input"
            />
            <button class="btn-trending" (click)="showTrending()" [title]="t('search.trending')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
              </svg>
              {{ t('search.trending') }}
            </button>
          </div>

          <div class="filters">
            <select [ngModel]="selectedGenre()" (ngModelChange)="onFilterChange('genre', $event)" class="select">
              <option [ngValue]="null">{{ t('filter.allGenres') }}</option>
              @for (genre of genres(); track genre.id) {
                <option [ngValue]="genre.id">{{ genre.name }}</option>
              }
            </select>

            <select [ngModel]="selectedDecade()" (ngModelChange)="onFilterChange('decade', $event)" class="select">
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

            <select [ngModel]="selectedRating()" (ngModelChange)="onFilterChange('rating', $event)" class="select">
              <option [ngValue]="null">{{ t('filter.minRating') }}</option>
              <option [ngValue]="6">6+</option>
              <option [ngValue]="7">7+</option>
              <option [ngValue]="8">8+</option>
              <option [ngValue]="9">9+</option>
            </select>

            <select [ngModel]="selectedSort()" (ngModelChange)="onFilterChange('sort', $event)" class="select">
              <option [ngValue]="null">{{ t('filter.sortPopularity') }}</option>
              <option value="vote_average.desc">{{ t('filter.sortRating') }}</option>
              <option value="primary_release_date.desc">{{ t('filter.sortRecent') }}</option>
              <option value="revenue.desc">{{ t('filter.sortRevenue') }}</option>
            </select>

            <select [ngModel]="selectedLanguage()" (ngModelChange)="onFilterChange('language', $event)" class="select">
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

            <select [ngModel]="selectedRuntime()" (ngModelChange)="onFilterChange('runtime', $event)" class="select">
              <option [ngValue]="null">{{ t('filter.allDurations') }}</option>
              <option value="short">{{ t('filter.short') }}</option>
              <option value="medium">{{ t('filter.medium') }}</option>
              <option value="long">{{ t('filter.long') }}</option>
            </select>

            <div class="director-autocomplete">
              <input
                type="text"
                [ngModel]="directorQuery()"
                (ngModelChange)="onDirectorInput($event)"
                (focus)="onDirectorFocus()"
                (blur)="hideDirectorDropdown()"
                [placeholder]="selectedDirector() ? '' : t('filter.directorPlaceholder')"
                class="input"
              />
              @if (selectedDirector()) {
                <span class="director-tag">
                  {{ selectedDirector()!.name }}
                  <button class="tag-remove" (click)="clearDirectorAndApply()" type="button">&times;</button>
                </span>
              }
              @if (showDirectorDropdown() && directorResults().length > 0) {
                <div class="dropdown-list">
                  @for (person of directorResults(); track person.id) {
                    <div class="dropdown-item" (mousedown)="selectDirectorAndApply(person)">
                      <span class="dropdown-name">{{ person.name }}</span>
                      @if (person.known_for && person.known_for.length > 0) {
                        <span class="dropdown-hint">{{ person.known_for[0].title }}</span>
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

          <!-- Results area: opacity fade during loading to prevent layout jump -->
          <div class="results-area" [class.is-loading]="loading()">

            @if (hasResults()) {
              <div class="results-count">{{ totalResults() }} {{ t('search.resultsCount') }}</div>
              <div class="movie-grid">
                @for (movie of movieResults(); track movie.id) {
                  <app-movie-card [movie]="movie" />
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
                </div>
              }
            }

            @if (!hasResults() && !searched() && defaultMovies().length > 0) {
              <div class="default-section">
                <h3 class="default-title">{{ t('search.trendingTitle') }}</h3>
                <div class="movie-grid">
                  @for (movie of defaultMovies(); track movie.id) {
                    <app-movie-card [movie]="movie" />
                  }
                </div>
              </div>
            }

            @if (loading() && !hasResults()) {
              <div class="loader"><div class="spinner"></div></div>
            }

            @if (!loading() && searched() && !hasResults()) {
              <div class="empty-state"><p>{{ t('search.noResults') }}</p></div>
            }
          </div>
        </div>
      }

      @if (activeTab() === 'actor') {
        <app-actor-results />
      }

      @if (activeTab() === 'scene') {
        <app-scene-search />
      }
    </div>
  `,
  styleUrl: './search.component.scss'
})
export class SearchComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);

  // --- UI state ---
  activeTab = signal<SearchTab>('movie');
  movieQuery = signal('');
  movieResults = signal<Movie[]>([]);
  genres = signal<Genre[]>([]);
  loading = signal(false);
  searched = signal(false);
  totalResults = signal(0);
  currentPage = signal(1);
  totalPages = signal(0);
  searchMode = signal<SearchMode>('none');
  defaultMovies = signal<Movie[]>([]);

  // --- Filters ---
  selectedGenre = signal<number | null>(null);
  selectedRating = signal<number | null>(null);
  selectedSort = signal<string | null>(null);
  selectedLanguage = signal<string | null>(null);
  selectedRuntime = signal<string | null>(null);
  selectedDecade = signal<string | null>(null);

  // --- Director autocomplete ---
  directorQuery = signal('');
  directorResults = signal<Person[]>([]);
  selectedDirector = signal<Person | null>(null);
  showDirectorDropdown = signal(false);

  // --- Internal cleanup handles ---
  private textSearchTimeout: any;
  private directorSearchTimeout: any;
  private activeRequest?: Subscription;

  t(key: string): string { return this.ts.t(key); }

  /** True when movieResults has items. */
  hasResults(): boolean {
    return this.movieResults().length > 0;
  }

  /** Computes visible page numbers with ellipsis markers (-1). */
  visiblePages(): number[] {
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
  }

  // =====================
  //  Lifecycle
  // =====================

  ngOnInit(): void {
    this.movieService.getGenres().subscribe(res => this.genres.set(res.genres));

    this.route.queryParams.subscribe(params => {
      // Always sync tab from URL — defaults to 'movie' when absent
      this.activeTab.set((params['tab'] as SearchTab) || 'movie');

      if (params['q']) {
        this.movieQuery.set(params['q']);
        if (this.activeTab() === 'movie') this.executeTextSearch(1);
      } else if (this.activeTab() === 'movie') {
        this.loadDefaultMovies();
      }
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.textSearchTimeout);
    clearTimeout(this.directorSearchTimeout);
    this.activeRequest?.unsubscribe();
  }

  // =====================
  //  Default content
  // =====================

  private loadDefaultMovies(): void {
    this.movieService.getTrending().subscribe({
      next: res => this.defaultMovies.set(res.results),
      error: () => {}
    });
  }

  // =====================
  //  Tab navigation
  // =====================

  setTab(tab: SearchTab): void {
    this.activeTab.set(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge'
    });
    if (tab === 'movie' && !this.searched() && this.defaultMovies().length === 0) {
      this.loadDefaultMovies();
    }
  }

  // =====================
  //  Debounced text search
  // =====================

  /** Handles text input with 400ms debounce. */
  onQueryInput(value: string): void {
    this.movieQuery.set(value);
    clearTimeout(this.textSearchTimeout);

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.searchMode() === 'text' || !this.searched()) {
        this.searched.set(false);
        this.movieResults.set([]);
        this.totalResults.set(0);
        this.currentPage.set(1);
        this.totalPages.set(0);
        this.searchMode.set('none');
        if (this.defaultMovies().length === 0) this.loadDefaultMovies();
      }
      return;
    }

    this.textSearchTimeout = setTimeout(() => this.executeTextSearch(1), 400);
  }

  /** Immediate search triggered by Enter key. */
  searchNow(): void {
    clearTimeout(this.textSearchTimeout);
    const trimmed = this.movieQuery().trim();
    if (trimmed.length >= 2) this.executeTextSearch(1);
  }

  private executeTextSearch(page: number): void {
    const query = this.movieQuery().trim();
    if (!query) return;

    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.searched.set(true);
    this.searchMode.set('text');
    this.currentPage.set(page);

    this.activeRequest = this.movieService.searchMovies(query, page).subscribe({
      next: res => {
        this.movieResults.set(res.results);
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

  /** Updates a single filter and triggers a discover request. */
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
    this.executeDiscover(1);
  }

  /** Builds TMDB discover params from current filter state. */
  private buildDiscoverParams(page: number) {
    const runtime = this.selectedRuntime();
    let runtimeGte: number | undefined;
    let runtimeLte: number | undefined;
    if (runtime === 'short') runtimeLte = 90;
    else if (runtime === 'medium') { runtimeGte = 90; runtimeLte = 120; }
    else if (runtime === 'long') runtimeGte = 120;

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
      directorId: this.selectedDirector()?.id ?? undefined,
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

    this.activeRequest = this.movieService.discoverMovies(this.buildDiscoverParams(page)).subscribe({
      next: res => {
        this.movieResults.set(res.results);
        this.totalResults.set(res.total_results);
        this.totalPages.set(Math.min(res.total_pages, 500));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // =====================
  //  Pagination
  // =====================

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (this.searchMode() === 'text') this.executeTextSearch(page);
    else this.executeDiscover(page);
  }

  // =====================
  //  Trending / Reset
  // =====================

  showTrending(): void {
    this.resetFilters();
  }

  /** Clears all filters, query, and reloads trending movies. */
  resetFilters(): void {
    this.selectedGenre.set(null);
    this.selectedRating.set(null);
    this.selectedSort.set(null);
    this.selectedLanguage.set(null);
    this.selectedRuntime.set(null);
    this.selectedDecade.set(null);
    this.clearDirector();
    this.movieQuery.set('');
    this.searched.set(false);
    this.searchMode.set('none');
    this.movieResults.set([]);
    this.totalResults.set(0);
    this.currentPage.set(1);
    this.totalPages.set(0);
    this.loadDefaultMovies();
  }

  // =====================
  //  Director autocomplete
  // =====================

  /** Debounced director search (300ms). */
  onDirectorInput(value: string): void {
    this.directorQuery.set(value);
    if (this.selectedDirector()) this.selectedDirector.set(null);

    clearTimeout(this.directorSearchTimeout);
    if (value.trim().length >= 2) {
      this.directorSearchTimeout = setTimeout(() => {
        this.movieService.searchPersons(value.trim()).subscribe(res => {
          const directors = res.results
            .filter(p => p.known_for_department === 'Directing')
            .slice(0, 8);
          this.directorResults.set(directors);
          this.showDirectorDropdown.set(directors.length > 0);
        });
      }, 300);
    } else {
      this.directorResults.set([]);
      this.showDirectorDropdown.set(false);
    }
  }

  onDirectorFocus(): void {
    if (this.directorResults().length > 0 && !this.selectedDirector()) {
      this.showDirectorDropdown.set(true);
    }
  }

  selectDirectorAndApply(person: Person): void {
    this.selectedDirector.set(person);
    this.directorQuery.set(person.name);
    this.showDirectorDropdown.set(false);
    this.directorResults.set([]);
    this.currentPage.set(1);
    this.executeDiscover(1);
  }

  clearDirectorAndApply(): void {
    this.clearDirector();
    this.currentPage.set(1);
    if (this.searched()) this.executeDiscover(1);
  }

  private clearDirector(): void {
    this.selectedDirector.set(null);
    this.directorQuery.set('');
    this.directorResults.set([]);
    this.showDirectorDropdown.set(false);
  }

  hideDirectorDropdown(): void {
    setTimeout(() => this.showDirectorDropdown.set(false), 200);
  }
}
