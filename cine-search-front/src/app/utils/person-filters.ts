import { Person } from '../models/movie.model';

/** TMDB genre IDs for filtering persons by their known_for movies. */
export const GENRE_IDS: Record<string, number> = {
  action: 28, comedy: 35, drama: 18, horror: 27, scifi: 878,
  romance: 10749, thriller: 53, animation: 16, adventure: 12,
  fantasy: 14, crime: 80, family: 10751, documentary: 99,
  war: 10752, history: 36, music: 10402, western: 37, mystery: 9648
};

export interface PersonFilterOptions {
  gender: number;
  genre: string;
  country: string;
  sort: string;
}

/** Filters and sorts a list of persons based on the given criteria. */
export function applyPersonFilters(persons: Person[], filters: PersonFilterOptions): Person[] {
  let result = [...persons];

  if (filters.gender !== 0) {
    result = result.filter(p => p.gender === filters.gender);
  }

  if (filters.genre) {
    const genreId = GENRE_IDS[filters.genre];
    if (genreId) {
      result = result.filter(p =>
        p.known_for?.some(m => m.genre_ids?.includes(genreId))
      );
    }
  }

  if (filters.country) {
    result = result.filter(p =>
      p.known_for?.some(m => m.original_language === filters.country)
    );
  }

  if (filters.sort === 'nameAZ') {
    result.sort((a, b) => a.name.localeCompare(b.name));
  } else if (filters.sort === 'nameZA') {
    result.sort((a, b) => b.name.localeCompare(a.name));
  } else {
    result.sort((a, b) => b.popularity - a.popularity);
  }

  return result;
}
