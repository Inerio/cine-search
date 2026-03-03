import {
  Component,
  inject,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
  DestroyRef,
} from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { Location } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Observable, forkJoin, Subscription } from "rxjs";
import { MovieCardComponent } from "../movie-card/movie-card.component";
import { MovieService } from "../../services/movie.service";
import { ImageService } from "../../services/image.service";
import { TranslationService } from "../../services/translation.service";
import {
  Movie,
  Genre,
  Person,
  PersonSearchResponse,
} from "../../models/movie.model";
import { computeVisiblePages } from "../../utils/pagination";
import { applyPersonFilters } from "../../utils/person-filters";
import {
  filterAndSortFilmography,
  FilmSort,
} from "../../utils/filmography-filters";
import {
  PERSON_PAGE_SIZE,
  PERSON_BATCH_SIZE,
  SEARCH_DEBOUNCE_MS,
} from "../../utils/constants";

type ActorMode = "popular" | "trending" | "search" | "filter";

const TMDB_PER_PAGE = 3; // 3 TMDB pages (60 actors) → display 36
const MAX_TMDB_PAGES = 100; // Safety cap: 2000 actors max

@Component({
  selector: "app-actor-results",
  standalone: true,
  imports: [FormsModule, MovieCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
        <button
          class="btn-trending"
          (click)="showTrending()"
          [title]="t('actor.trending')"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
          {{ t("actor.trending") }}
        </button>
      </div>

      @if (!selectedActor()) {
        <!-- Person filters -->
        <div class="filters">
          <select
            class="filter-select"
            [ngModel]="selectedGender()"
            (ngModelChange)="selectedGender.set(+$event); onFilterChange()"
          >
            <option [ngValue]="0">{{ t("actor.filter.allGenders") }}</option>
            <option [ngValue]="2">{{ t("actor.filter.male") }}</option>
            <option [ngValue]="1">{{ t("actor.filter.female") }}</option>
            <option [ngValue]="3">{{ t("actor.filter.nonBinary") }}</option>
          </select>

          <select
            class="filter-select"
            [ngModel]="selectedGenre()"
            (ngModelChange)="selectedGenre.set($event); onFilterChange()"
          >
            <option value="">{{ t("actor.filter.allGenres") }}</option>
            <option value="action">{{ t("actor.filter.genre.action") }}</option>
            <option value="comedy">{{ t("actor.filter.genre.comedy") }}</option>
            <option value="drama">{{ t("actor.filter.genre.drama") }}</option>
            <option value="horror">{{ t("actor.filter.genre.horror") }}</option>
            <option value="scifi">{{ t("actor.filter.genre.scifi") }}</option>
            <option value="romance">
              {{ t("actor.filter.genre.romance") }}
            </option>
            <option value="thriller">
              {{ t("actor.filter.genre.thriller") }}
            </option>
            <option value="animation">
              {{ t("actor.filter.genre.animation") }}
            </option>
            <option value="adventure">
              {{ t("actor.filter.genre.adventure") }}
            </option>
            <option value="fantasy">
              {{ t("actor.filter.genre.fantasy") }}
            </option>
            <option value="crime">{{ t("actor.filter.genre.crime") }}</option>
            <option value="family">{{ t("actor.filter.genre.family") }}</option>
            <option value="documentary">
              {{ t("actor.filter.genre.documentary") }}
            </option>
            <option value="war">{{ t("actor.filter.genre.war") }}</option>
            <option value="history">
              {{ t("actor.filter.genre.history") }}
            </option>
            <option value="music">{{ t("actor.filter.genre.music") }}</option>
            <option value="western">
              {{ t("actor.filter.genre.western") }}
            </option>
            <option value="mystery">
              {{ t("actor.filter.genre.mystery") }}
            </option>
          </select>

          <select
            class="filter-select"
            [ngModel]="selectedCountry()"
            (ngModelChange)="selectedCountry.set($event); onFilterChange()"
          >
            <option value="">{{ t("actor.filter.allCountries") }}</option>
            <option value="en">{{ t("actor.filter.country.en") }}</option>
            <option value="fr">{{ t("actor.filter.country.fr") }}</option>
            <option value="ja">{{ t("actor.filter.country.ja") }}</option>
            <option value="ko">{{ t("actor.filter.country.ko") }}</option>
            <option value="es">{{ t("actor.filter.country.es") }}</option>
            <option value="de">{{ t("actor.filter.country.de") }}</option>
            <option value="it">{{ t("actor.filter.country.it") }}</option>
            <option value="pt">{{ t("actor.filter.country.pt") }}</option>
            <option value="hi">{{ t("actor.filter.country.hi") }}</option>
            <option value="zh">{{ t("actor.filter.country.zh") }}</option>
            <option value="ru">{{ t("actor.filter.country.ru") }}</option>
            <option value="sv">{{ t("actor.filter.country.sv") }}</option>
            <option value="tr">{{ t("actor.filter.country.tr") }}</option>
          </select>

          <select
            class="filter-select"
            [ngModel]="selectedSort()"
            (ngModelChange)="selectedSort.set($event); onFilterChange()"
          >
            <option value="popularity">
              {{ t("actor.filter.sortPopularity") }}
            </option>
            <option value="nameAZ">{{ t("actor.filter.sortNameAZ") }}</option>
            <option value="nameZA">{{ t("actor.filter.sortNameZA") }}</option>
          </select>

          @if (hasActiveFilters()) {
            <button class="btn-reset" (click)="resetFilters()">
              {{ t("actor.reset") }}
            </button>
          }
        </div>
      }

      @if (selectedActor()) {
        <!-- Filmography filters -->
        <div class="filters">
          <select
            [ngModel]="filmGenre()"
            (ngModelChange)="filmGenre.set($event)"
            class="filter-select"
          >
            <option [ngValue]="null">{{ t("filter.allGenres") }}</option>
            @for (genre of genres(); track genre.id) {
              <option [ngValue]="genre.id">{{ genre.name }}</option>
            }
          </select>

          <select
            [ngModel]="filmDecade()"
            (ngModelChange)="filmDecade.set($event)"
            class="filter-select"
          >
            <option [ngValue]="null">{{ t("filter.allPeriods") }}</option>
            <option value="2020">2020s</option>
            <option value="2010">2010s</option>
            <option value="2000">2000s</option>
            <option value="1990">1990s</option>
            <option value="1980">1980s</option>
            <option value="1970">1970s</option>
            <option value="1960">1960s</option>
            <option value="1900">{{ t("filter.before1960") }}</option>
          </select>

          <select
            [ngModel]="filmRating()"
            (ngModelChange)="filmRating.set($event)"
            class="filter-select"
          >
            <option [ngValue]="null">{{ t("filter.minRating") }}</option>
            <option [ngValue]="6">6+</option>
            <option [ngValue]="7">7+</option>
            <option [ngValue]="8">8+</option>
            <option [ngValue]="9">9+</option>
          </select>

          <select
            [ngModel]="filmSort()"
            (ngModelChange)="filmSort.set($event)"
            class="filter-select"
          >
            <option value="popularity">{{ t("filter.sortPopularity") }}</option>
            <option value="vote_average">{{ t("filter.sortRating") }}</option>
            <option value="recent">{{ t("filter.sortRecent") }}</option>
          </select>

          <select
            [ngModel]="filmLanguage()"
            (ngModelChange)="filmLanguage.set($event)"
            class="filter-select"
          >
            <option [ngValue]="null">{{ t("filter.allLanguages") }}</option>
            <option value="fr">{{ t("filter.lang.fr") }}</option>
            <option value="en">{{ t("filter.lang.en") }}</option>
            <option value="ja">{{ t("filter.lang.ja") }}</option>
            <option value="ko">{{ t("filter.lang.ko") }}</option>
            <option value="es">{{ t("filter.lang.es") }}</option>
            <option value="de">{{ t("filter.lang.de") }}</option>
            <option value="it">{{ t("filter.lang.it") }}</option>
            <option value="pt">{{ t("filter.lang.pt") }}</option>
            <option value="hi">{{ t("filter.lang.hi") }}</option>
            <option value="zh">{{ t("filter.lang.zh") }}</option>
            <option value="ru">{{ t("filter.lang.ru") }}</option>
            <option value="sv">{{ t("filter.lang.sv") }}</option>
            <option value="tr">{{ t("filter.lang.tr") }}</option>
          </select>

          @if (hasActiveFilmFilters()) {
            <button class="btn-reset" (click)="resetFilmFilters()">
              {{ t("search.reset") }}
            </button>
          }
        </div>
      }

      <!-- Results area -->
      <div
        class="results-area"
        [class.is-loading]="loading() && displayedActors().length > 0"
      >
        @if (selectedActor()) {
          <div class="selected-person">
            <button class="back-btn" (click)="goBack()">
              &#8592; {{ t("actor.back") }}
            </button>
            <div class="person-header">
              <img
                [src]="
                  imageService.getProfileUrl(
                    selectedActor()!.profile_path,
                    'w342'
                  )
                "
                [alt]="selectedActor()!.name"
                class="person-photo-large"
              />
              <div>
                <h2>{{ selectedActor()!.name }}</h2>
                <p class="filmography-count">
                  {{ filteredFilmography().length }} {{ t("actor.films") }}
                </p>
              </div>
            </div>
            <div class="movie-grid">
              @for (movie of filteredFilmography(); track movie.id) {
                <app-movie-card [movie]="movie" />
              }
            </div>
          </div>
        }

        @if (!selectedActor()) {
          @if (displayedActors().length > 0 || batchLoading()) {
            <div class="results-info">
              <h3 class="default-title">{{ modeTitle() }}</h3>
              @if (totalResults() > 0) {
                <span class="results-count"
                  >{{ totalResults() }} {{ t("actor.resultsCount") }}</span
                >
              }
            </div>
          }

          @if (displayedActors().length > 0) {
            <div class="person-grid">
              @for (actor of displayedActors(); track actor.id) {
                <div
                  class="person-card"
                  tabindex="0"
                  (click)="selectActor(actor)"
                  (keydown.enter)="selectActor(actor)"
                >
                  <img
                    [src]="imageService.getProfileUrl(actor.profile_path)"
                    [alt]="actor.name"
                    class="person-photo"
                  />
                  <div class="person-info">
                    <h3>{{ actor.name }}</h3>
                    <span class="department">{{
                      actor.known_for_department
                    }}</span>
                  </div>
                </div>
              }
            </div>
          }

          @if (batchLoading() && displayedActors().length > 0) {
            <div class="batch-loading-hint">{{ t("actor.loadingMore") }}</div>
          }

          @if (totalPages() > 1 && !loading()) {
            <div class="pagination">
              <button
                class="page-btn"
                [disabled]="currentPage() <= 1"
                (click)="goToPage(currentPage() - 1)"
              >
                &#8592; {{ t("search.previous") }}
              </button>
              @for (p of visiblePages(); track $index) {
                @if (p === -1) {
                  <span class="page-ellipsis">...</span>
                } @else {
                  <button
                    class="page-num"
                    [class.active]="p === currentPage()"
                    (click)="goToPage(p)"
                  >
                    {{ p }}
                  </button>
                }
              }
              <button
                class="page-btn"
                [disabled]="currentPage() >= totalPages()"
                (click)="goToPage(currentPage() + 1)"
              >
                {{ t("search.next") }} &#8594;
              </button>
              <input
                type="number"
                class="page-jump-input"
                [min]="1"
                [max]="totalPages()"
                [placeholder]="currentPage() + '/' + totalPages()"
                (keyup.enter)="onPageJump($event)"
              />
            </div>
          }

          @if (loading() && displayedActors().length === 0 && !batchLoading()) {
            <div class="loader"><div class="spinner"></div></div>
          }

          @if (
            !loading() && !batchLoading() && displayedActors().length === 0
          ) {
            <div class="empty-state">
              <p>{{ t("actor.noResults") }}</p>
            </div>
          }
        }
      </div>
    </div>
  `,
  styleUrl: "./actor-results.component.scss",
})
export class ActorResultsComponent implements OnInit {
  private movieService = inject(MovieService);
  private ts = inject(TranslationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private destroyRef = inject(DestroyRef);
  imageService = inject(ImageService);

  // --- Mode ---
  mode = signal<ActorMode>("popular");

  // --- Pagination ---
  currentPage = signal(1);
  totalPages = signal(0);
  totalResults = signal(0);

  // --- Search ---
  actorQuery = signal("");

  // --- Filters ---
  selectedGender = signal(0);
  selectedGenre = signal("");
  selectedCountry = signal("");
  selectedSort = signal("popularity");

  // --- Actor detail ---
  selectedActor = signal<Person | null>(null);
  actorMovies = signal<Movie[]>([]);

  // --- Filmography filters (when person selected) ---
  genres = signal<Genre[]>([]);
  filmGenre = signal<number | null>(null);
  filmDecade = signal<string | null>(null);
  filmRating = signal<number | null>(null);
  filmSort = signal<FilmSort>("popularity");
  filmLanguage = signal<string | null>(null);

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
    if (this.mode() !== "filter") return this.serverPageActors();
    return applyPersonFilters(this.cachedActors(), {
      gender: this.selectedGender(),
      genre: this.selectedGenre(),
      country: this.selectedCountry(),
      sort: this.selectedSort(),
    });
  });

  // --- Computed: actors to display on current page ---
  displayedActors = computed(() => {
    if (this.mode() !== "filter") return this.allFilteredActors();
    const start = (this.currentPage() - 1) * PERSON_PAGE_SIZE;
    return this.allFilteredActors().slice(start, start + PERSON_PAGE_SIZE);
  });

  visiblePages = computed(() =>
    computeVisiblePages(this.totalPages(), this.currentPage()),
  );

  hasActiveFilters = computed(
    () =>
      this.selectedGender() !== 0 ||
      this.selectedGenre() !== "" ||
      this.selectedCountry() !== "" ||
      this.selectedSort() !== "popularity",
  );

  filteredFilmography = computed(() =>
    filterAndSortFilmography(this.actorMovies(), {
      genre: this.filmGenre(),
      decade: this.filmDecade(),
      rating: this.filmRating(),
      language: this.filmLanguage(),
      sort: this.filmSort(),
    }),
  );

  hasActiveFilmFilters = computed(
    () =>
      this.filmGenre() !== null ||
      this.filmDecade() !== null ||
      this.filmRating() !== null ||
      this.filmSort() !== "popularity" ||
      this.filmLanguage() !== null,
  );

  modeTitle = computed(() => {
    switch (this.mode()) {
      case "popular":
        return this.t("actor.popularTitle");
      case "trending":
        return this.t("actor.trendingTitle");
      case "search":
        return this.t("actor.searchResults");
      case "filter":
        return this.t("actor.filterResults");
    }
  });

  private searchTimeout: ReturnType<typeof setTimeout> | undefined;
  private activeRequest?: Subscription;

  t(key: string): string {
    return this.ts.t(key);
  }

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.searchTimeout);
      this.activeRequest?.unsubscribe();
    });

    this.movieService
      .getGenres()
      .subscribe((res) => this.genres.set(res.genres));

    const personId = this.route.snapshot.queryParamMap.get("personId");
    if (personId && this.route.snapshot.queryParamMap.get("tab") === "actor") {
      this.loading.set(true);
      this.movieService.getPersonDetails(+personId).subscribe({
        next: (person) => this.selectActor(person),
        error: () => {
          this.loading.set(false);
          this.loadServerPage(1);
        },
      });
    } else {
      this.loadServerPage(1);
    }
  }

  // ==================================================
  //  Server-side pagination (popular / trending / search)
  // ==================================================

  private getDataSource(): (page: number) => Observable<PersonSearchResponse> {
    const mode = this.mode();
    if (mode === "search") {
      const query = this.actorQuery().trim();
      return (page) => this.movieService.searchPersons(query, page);
    }
    if (mode === "trending") {
      return (page) => this.movieService.getTrendingActors(page);
    }
    return (page) => this.movieService.getPopularActors(page);
  }

  private loadServerPage(page: number): void {
    this.activeRequest?.unsubscribe();
    this.loading.set(true);
    this.currentPage.set(page);

    const source = this.getDataSource();
    const startTmdbPage = (page - 1) * TMDB_PER_PAGE + 1;
    const requests = Array.from({ length: TMDB_PER_PAGE }, (_, i) =>
      source(startTmdbPage + i),
    );

    this.activeRequest = forkJoin(requests).subscribe({
      next: (responses) => {
        const all = responses.flatMap((r) => r.results);
        const seen = new Set<number>();
        const actors = all
          .filter((p) => {
            if (
              seen.has(p.id) ||
              !p.profile_path ||
              p.known_for_department !== "Acting"
            )
              return false;
            seen.add(p.id);
            return true;
          })
          .slice(0, PERSON_PAGE_SIZE);

        this.serverPageActors.set(actors);
        const tmdbTotalPages = Math.min(responses[0].total_pages, 500);
        this.totalPages.set(Math.ceil(tmdbTotalPages / TMDB_PER_PAGE));
        this.totalResults.set(responses[0].total_results);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
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

    const endPage = Math.min(
      startPage + PERSON_BATCH_SIZE - 1,
      this.maxAvailablePages(),
      MAX_TMDB_PAGES,
    );
    const requests: Observable<PersonSearchResponse>[] = [];
    for (let p = startPage; p <= endPage; p++) {
      requests.push(this.movieService.getPopularActors(p));
    }

    this.activeRequest?.unsubscribe();
    this.activeRequest = forkJoin(requests).subscribe({
      next: (responses) => {
        const existing = this.cachedActors();
        const seen = new Set(existing.map((a) => a.id));
        const newActors: Person[] = [];

        for (const res of responses) {
          this.maxAvailablePages.set(Math.min(res.total_pages, 500));
          for (const actor of res.results) {
            if (
              !seen.has(actor.id) &&
              actor.profile_path &&
              actor.known_for_department === "Acting"
            ) {
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

        const filtered = this.allFilteredActors();
        const neededForPage = this.currentPage() * PERSON_PAGE_SIZE;
        if (
          filtered.length < neededForPage &&
          endPage < Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)
        ) {
          this.loadFilterBatch(endPage + 1);
        }
      },
      error: () => {
        this.batchLoading.set(false);
        this.loading.set(false);
      },
    });
  }

  private rebuildFilteredList(): void {
    const filtered = this.allFilteredActors();
    this.totalResults.set(filtered.length);
    this.totalPages.set(
      Math.max(1, Math.ceil(filtered.length / PERSON_PAGE_SIZE)),
    );
    if (this.currentPage() > this.totalPages()) {
      this.currentPage.set(Math.max(1, this.totalPages()));
    }
  }

  onFilterChange(): void {
    this.currentPage.set(1);

    if (this.hasActiveFilters()) {
      if (this.mode() !== "filter") {
        this.mode.set("filter");
        this.cachedActors.set([]);
        this.maxLoadedPage.set(0);
        this.maxAvailablePages.set(500);
        this.loadFilterBatch(1);
      } else {
        this.rebuildFilteredList();
        const filtered = this.allFilteredActors();
        if (
          filtered.length < PERSON_PAGE_SIZE &&
          this.maxLoadedPage() <
            Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)
        ) {
          this.loadFilterBatch(this.maxLoadedPage() + 1);
        }
      }
    } else {
      this.cachedActors.set([]);
      this.maxLoadedPage.set(0);
      if (this.actorQuery().trim().length >= 2) {
        this.mode.set("search");
        this.loadServerPage(1);
      } else {
        this.mode.set("popular");
        this.loadServerPage(1);
      }
    }
  }

  resetFilters(): void {
    this.selectedGender.set(0);
    this.selectedGenre.set("");
    this.selectedCountry.set("");
    this.selectedSort.set("popularity");
    this.onFilterChange();
  }

  resetFilmFilters(): void {
    this.filmGenre.set(null);
    this.filmDecade.set(null);
    this.filmRating.set(null);
    this.filmSort.set("popularity");
    this.filmLanguage.set(null);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage())
      return;

    if (this.mode() === "filter") {
      const neededActors = page * PERSON_PAGE_SIZE;
      const haveActors = this.allFilteredActors().length;
      this.currentPage.set(page);
      if (
        neededActors > haveActors &&
        this.maxLoadedPage() <
          Math.min(this.maxAvailablePages(), MAX_TMDB_PAGES)
      ) {
        this.loadFilterBatch(this.maxLoadedPage() + 1);
      }
    } else {
      this.loadServerPage(page);
    }

    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  onPageJump(event: Event): void {
    const input = event.target as HTMLInputElement;
    const page = parseInt(input.value, 10);
    if (page >= 1 && page <= this.totalPages()) {
      this.goToPage(page);
    }
    input.value = "";
  }

  showTrending(): void {
    this.actorQuery.set("");
    this.selectedActor.set(null);
    this.actorMovies.set([]);
    this.selectedGender.set(0);
    this.selectedGenre.set("");
    this.selectedCountry.set("");
    this.selectedSort.set("popularity");
    this.cachedActors.set([]);
    this.maxLoadedPage.set(0);
    this.mode.set("trending");
    this.loadServerPage(1);
  }

  onQueryInput(value: string): void {
    this.actorQuery.set(value);
    clearTimeout(this.searchTimeout);

    if (this.selectedActor()) {
      this.selectedActor.set(null);
      this.actorMovies.set([]);
    }

    const trimmed = value.trim();
    if (trimmed.length < 2) {
      if (this.mode() === "search") {
        if (this.hasActiveFilters()) {
          this.mode.set("filter");
          this.cachedActors.set([]);
          this.maxLoadedPage.set(0);
          this.currentPage.set(1);
          this.loadFilterBatch(1);
        } else {
          this.mode.set("popular");
          this.loadServerPage(1);
        }
      }
      return;
    }

    this.searchTimeout = setTimeout(
      () => this.executeSearch(),
      SEARCH_DEBOUNCE_MS,
    );
  }

  searchNow(): void {
    clearTimeout(this.searchTimeout);
    if (this.actorQuery().trim().length >= 2) this.executeSearch();
  }

  private executeSearch(): void {
    this.activeRequest?.unsubscribe();
    this.mode.set("search");
    this.currentPage.set(1);
    this.cachedActors.set([]);
    this.maxLoadedPage.set(0);
    this.loadServerPage(1);
  }

  selectActor(actor: Person): void {
    this.selectedActor.set(actor);
    this.loading.set(true);

    // Persist personId in URL so back-navigation restores the actor
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { personId: actor.id },
      queryParamsHandling: "merge",
      replaceUrl: true,
    });

    this.activeRequest?.unsubscribe();
    this.activeRequest = this.movieService.getPersonMovies(actor.id).subscribe({
      next: (res) => {
        const sorted = [...res.cast]
          .filter((m) => m.poster_path)
          .sort((a, b) => b.popularity - a.popularity);
        this.actorMovies.set(sorted);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goBack(): void {
    this.location.back();
  }

  clearSelection(): void {
    this.selectedActor.set(null);
    this.actorMovies.set([]);
    this.resetFilmFilters();

    // Remove personId from URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { personId: null },
      queryParamsHandling: "merge",
      replaceUrl: true,
    });
  }
}
