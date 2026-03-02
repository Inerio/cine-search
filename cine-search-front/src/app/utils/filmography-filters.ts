import { Movie } from '../models/movie.model';

/** Decade boundary: films before this year are grouped under "Avant 1960". */
const CLASSIC_ERA_BOUNDARY = 1960;
/** Sentinel value used in decade dropdowns for the "Avant 1960" option. */
const CLASSIC_ERA_SENTINEL = 1900;

export type FilmSort = 'popularity' | 'vote_average' | 'recent';

export interface FilmFilters {
  genre: number | null;
  decade: string | null;
  rating: number | null;
  language: string | null;
  sort: FilmSort;
}

/**
 * Applies genre, decade, rating, and language filters then sorts the result.
 * Shared between actor-results and director-results filmography panels.
 */
export function filterAndSortFilmography(movies: Movie[], filters: FilmFilters): Movie[] {
  let result = movies;

  if (filters.genre) {
    result = result.filter(m => m.genre_ids?.includes(filters.genre!));
  }

  if (filters.decade) {
    const y = parseInt(filters.decade, 10);
    if (y === CLASSIC_ERA_SENTINEL) {
      result = result.filter(m => {
        const yr = parseInt(m.release_date?.substring(0, 4), 10);
        return yr > 0 && yr < CLASSIC_ERA_BOUNDARY;
      });
    } else {
      result = result.filter(m => {
        const yr = parseInt(m.release_date?.substring(0, 4), 10);
        return yr >= y && yr <= y + 9;
      });
    }
  }

  if (filters.rating) {
    result = result.filter(m => m.vote_average >= filters.rating!);
  }

  if (filters.language) {
    result = result.filter(m => m.original_language === filters.language);
  }

  return [...result].sort((a, b) => {
    if (filters.sort === 'vote_average') return b.vote_average - a.vote_average;
    if (filters.sort === 'recent') return (b.release_date || '').localeCompare(a.release_date || '');
    return b.popularity - a.popularity;
  });
}
