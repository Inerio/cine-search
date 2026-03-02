/** Shared constants used across multiple components. */

// --- Pagination ---
export const PERSON_PAGE_SIZE = 36;
export const PERSON_BATCH_SIZE = 10;

// --- Debounce / Timers ---
export const SEARCH_DEBOUNCE_MS = 400;
export const DIRECTOR_SEARCH_DEBOUNCE_MS = 300;
export const DROPDOWN_HIDE_DELAY_MS = 200;

// --- Display limits ---
export const DROPDOWN_RESULTS_LIMIT = 8;
export const TOP_CAST_LIMIT = 10;

// --- Runtime thresholds (minutes) ---
export const MOVIE_RUNTIME = { SHORT_MAX: 90, MEDIUM_MIN: 90, MEDIUM_MAX: 120, LONG_MIN: 120 } as const;
export const TV_RUNTIME = { SHORT_MAX: 30, MEDIUM_MIN: 30, MEDIUM_MAX: 60, LONG_MIN: 60 } as const;

// --- Genre registries (TMDB IDs + i18n keys) ---
export const MOVIE_GENRES: readonly { id: number; key: string }[] = [
  { id: 28, key: 'filter.genre.action' },
  { id: 12, key: 'filter.genre.adventure' },
  { id: 16, key: 'filter.genre.animation' },
  { id: 35, key: 'filter.genre.comedy' },
  { id: 80, key: 'filter.genre.crime' },
  { id: 99, key: 'filter.genre.documentary' },
  { id: 18, key: 'filter.genre.drama' },
  { id: 10751, key: 'filter.genre.family' },
  { id: 14, key: 'filter.genre.fantasy' },
  { id: 36, key: 'filter.genre.history' },
  { id: 27, key: 'filter.genre.horror' },
  { id: 10402, key: 'filter.genre.music' },
  { id: 9648, key: 'filter.genre.mystery' },
  { id: 10749, key: 'filter.genre.romance' },
  { id: 878, key: 'filter.genre.scifi' },
  { id: 10770, key: 'filter.genre.tvMovie' },
  { id: 53, key: 'filter.genre.thriller' },
  { id: 10752, key: 'filter.genre.war' },
  { id: 37, key: 'filter.genre.western' },
];

export const TV_GENRES: readonly { id: number; key: string }[] = [
  { id: 10759, key: 'filter.genre.actionAdventure' },
  { id: 16, key: 'filter.genre.animation' },
  { id: 35, key: 'filter.genre.comedy' },
  { id: 80, key: 'filter.genre.crime' },
  { id: 99, key: 'filter.genre.documentary' },
  { id: 18, key: 'filter.genre.drama' },
  { id: 10751, key: 'filter.genre.family' },
  { id: 10762, key: 'filter.genre.kids' },
  { id: 9648, key: 'filter.genre.mystery' },
  { id: 10763, key: 'filter.genre.news' },
  { id: 10764, key: 'filter.genre.reality' },
  { id: 10765, key: 'filter.genre.scifiFantasy' },
  { id: 10766, key: 'filter.genre.soap' },
  { id: 10767, key: 'filter.genre.talk' },
  { id: 10768, key: 'filter.genre.warPolitics' },
  { id: 37, key: 'filter.genre.western' },
];
