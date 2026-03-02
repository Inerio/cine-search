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
