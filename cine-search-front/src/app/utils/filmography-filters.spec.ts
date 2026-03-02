import { filterAndSortFilmography, FilmFilters } from './filmography-filters';
import { Movie } from '../models/movie.model';

function makeMovie(overrides: Partial<Movie> = {}): Movie {
  return {
    id: 1,
    title: 'Test Movie',
    overview: '',
    poster_path: null,
    backdrop_path: null,
    release_date: '2020-06-15',
    vote_average: 7.0,
    vote_count: 100,
    popularity: 50,
    genre_ids: [28],
    original_language: 'en',
    ...overrides
  };
}

const noFilters: FilmFilters = { genre: null, decade: null, rating: null, language: null, sort: 'popularity' };

describe('filterAndSortFilmography', () => {
  it('should return all movies when no filters are active', () => {
    const movies = [makeMovie({ id: 1 }), makeMovie({ id: 2 })];
    expect(filterAndSortFilmography(movies, noFilters)).toHaveLength(2);
  });

  // --- Genre filter ---
  it('should filter by genre', () => {
    const movies = [
      makeMovie({ id: 1, genre_ids: [28, 12] }),
      makeMovie({ id: 2, genre_ids: [35] }),
      makeMovie({ id: 3, genre_ids: [28] })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, genre: 28 });
    expect(result).toHaveLength(2);
    expect(result.map(m => m.id)).toEqual(expect.arrayContaining([1, 3]));
  });

  // --- Decade filter ---
  it('should filter by specific decade', () => {
    const movies = [
      makeMovie({ id: 1, release_date: '2020-01-01' }),
      makeMovie({ id: 2, release_date: '2015-06-15' }),
      makeMovie({ id: 3, release_date: '2023-12-25' })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, decade: '2020' });
    expect(result).toHaveLength(2);
    expect(result.map(m => m.id)).toEqual(expect.arrayContaining([1, 3]));
  });

  it('should filter classic era (before 1960) using sentinel 1900', () => {
    const movies = [
      makeMovie({ id: 1, release_date: '1955-03-10' }),
      makeMovie({ id: 2, release_date: '1962-07-20' }),
      makeMovie({ id: 3, release_date: '1940-01-01' })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, decade: '1900' });
    expect(result).toHaveLength(2);
    expect(result.map(m => m.id)).toEqual(expect.arrayContaining([1, 3]));
  });

  // --- Rating filter ---
  it('should filter by minimum rating', () => {
    const movies = [
      makeMovie({ id: 1, vote_average: 8.5 }),
      makeMovie({ id: 2, vote_average: 5.0 }),
      makeMovie({ id: 3, vote_average: 7.0 })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, rating: 7 });
    expect(result).toHaveLength(2);
    expect(result.map(m => m.id)).toEqual(expect.arrayContaining([1, 3]));
  });

  // --- Language filter ---
  it('should filter by language', () => {
    const movies = [
      makeMovie({ id: 1, original_language: 'fr' }),
      makeMovie({ id: 2, original_language: 'en' }),
      makeMovie({ id: 3, original_language: 'fr' })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, language: 'fr' });
    expect(result).toHaveLength(2);
    expect(result.every(m => m.original_language === 'fr')).toBe(true);
  });

  // --- Sorting ---
  it('should sort by popularity (default)', () => {
    const movies = [
      makeMovie({ id: 1, popularity: 10 }),
      makeMovie({ id: 2, popularity: 50 }),
      makeMovie({ id: 3, popularity: 30 })
    ];
    const result = filterAndSortFilmography(movies, noFilters);
    expect(result.map(m => m.id)).toEqual([2, 3, 1]);
  });

  it('should sort by vote_average', () => {
    const movies = [
      makeMovie({ id: 1, vote_average: 6.0 }),
      makeMovie({ id: 2, vote_average: 9.0 }),
      makeMovie({ id: 3, vote_average: 7.5 })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, sort: 'vote_average' });
    expect(result.map(m => m.id)).toEqual([2, 3, 1]);
  });

  it('should sort by recent release date', () => {
    const movies = [
      makeMovie({ id: 1, release_date: '2018-01-01' }),
      makeMovie({ id: 2, release_date: '2023-06-15' }),
      makeMovie({ id: 3, release_date: '2020-12-25' })
    ];
    const result = filterAndSortFilmography(movies, { ...noFilters, sort: 'recent' });
    expect(result.map(m => m.id)).toEqual([2, 3, 1]);
  });

  // --- Combined filters ---
  it('should apply multiple filters together', () => {
    const movies = [
      makeMovie({ id: 1, genre_ids: [28], vote_average: 8.0, original_language: 'en', release_date: '2020-01-01' }),
      makeMovie({ id: 2, genre_ids: [28], vote_average: 5.0, original_language: 'en', release_date: '2020-06-01' }),
      makeMovie({ id: 3, genre_ids: [35], vote_average: 9.0, original_language: 'en', release_date: '2020-03-01' }),
      makeMovie({ id: 4, genre_ids: [28], vote_average: 9.0, original_language: 'fr', release_date: '2020-09-01' })
    ];
    const result = filterAndSortFilmography(movies, {
      genre: 28, decade: '2020', rating: 7, language: 'en', sort: 'vote_average'
    });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(1);
  });

  // --- Immutability ---
  it('should not mutate the original array', () => {
    const movies = [makeMovie({ id: 1, popularity: 10 }), makeMovie({ id: 2, popularity: 50 })];
    const original = [...movies];
    filterAndSortFilmography(movies, noFilters);
    expect(movies).toEqual(original);
  });
});
