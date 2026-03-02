import { applyPersonFilters, GENRE_IDS, PersonFilterOptions } from './person-filters';
import { Person } from '../models/movie.model';

function makePerson(overrides: Partial<Person> = {}): Person {
  return {
    id: 1,
    name: 'John Doe',
    profile_path: null,
    popularity: 50,
    gender: 2,
    known_for_department: 'Acting',
    known_for: [],
    ...overrides
  };
}

describe('applyPersonFilters', () => {
  const defaults: PersonFilterOptions = { gender: 0, genre: '', country: '', sort: 'popularity' };

  it('should return all persons when no filters active', () => {
    const persons = [makePerson({ id: 1 }), makePerson({ id: 2 })];
    expect(applyPersonFilters(persons, defaults)).toHaveLength(2);
  });

  it('should filter by gender', () => {
    const persons = [
      makePerson({ id: 1, gender: 1 }),
      makePerson({ id: 2, gender: 2 }),
      makePerson({ id: 3, gender: 1 })
    ];
    const result = applyPersonFilters(persons, { ...defaults, gender: 1 });
    expect(result).toHaveLength(2);
    expect(result.every(p => p.gender === 1)).toBe(true);
  });

  it('should filter by genre via known_for', () => {
    const persons = [
      makePerson({ id: 1, known_for: [{ id: 100, title: 'Action Movie', genre_ids: [28], overview: '', poster_path: null, backdrop_path: null, release_date: '2020-01-01', vote_average: 7, vote_count: 100, popularity: 50, original_language: 'en' }] }),
      makePerson({ id: 2, known_for: [{ id: 101, title: 'Comedy', genre_ids: [35], overview: '', poster_path: null, backdrop_path: null, release_date: '2020-01-01', vote_average: 6, vote_count: 80, popularity: 40, original_language: 'en' }] })
    ];
    const result = applyPersonFilters(persons, { ...defaults, genre: 'action' });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(1);
  });

  it('should filter by country via known_for original_language', () => {
    const persons = [
      makePerson({ id: 1, known_for: [{ id: 100, title: 'French Movie', genre_ids: [], overview: '', poster_path: null, backdrop_path: null, release_date: '', vote_average: 7, vote_count: 100, popularity: 50, original_language: 'fr' }] }),
      makePerson({ id: 2, known_for: [{ id: 101, title: 'English Movie', genre_ids: [], overview: '', poster_path: null, backdrop_path: null, release_date: '', vote_average: 6, vote_count: 80, popularity: 40, original_language: 'en' }] })
    ];
    const result = applyPersonFilters(persons, { ...defaults, country: 'fr' });
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(1);
  });

  it('should sort by popularity (default)', () => {
    const persons = [
      makePerson({ id: 1, popularity: 10 }),
      makePerson({ id: 2, popularity: 50 }),
      makePerson({ id: 3, popularity: 30 })
    ];
    const result = applyPersonFilters(persons, defaults);
    expect(result.map(p => p.id)).toEqual([2, 3, 1]);
  });

  it('should sort by name A-Z', () => {
    const persons = [
      makePerson({ id: 1, name: 'Charlie' }),
      makePerson({ id: 2, name: 'Alice' }),
      makePerson({ id: 3, name: 'Bob' })
    ];
    const result = applyPersonFilters(persons, { ...defaults, sort: 'nameAZ' });
    expect(result.map(p => p.name)).toEqual(['Alice', 'Bob', 'Charlie']);
  });

  it('should sort by name Z-A', () => {
    const persons = [
      makePerson({ id: 1, name: 'Alice' }),
      makePerson({ id: 2, name: 'Charlie' }),
      makePerson({ id: 3, name: 'Bob' })
    ];
    const result = applyPersonFilters(persons, { ...defaults, sort: 'nameZA' });
    expect(result.map(p => p.name)).toEqual(['Charlie', 'Bob', 'Alice']);
  });

  it('should not mutate the original array', () => {
    const persons = [makePerson({ id: 1 }), makePerson({ id: 2 })];
    const original = [...persons];
    applyPersonFilters(persons, { ...defaults, sort: 'nameAZ' });
    expect(persons).toEqual(original);
  });
});

describe('GENRE_IDS', () => {
  it('should contain expected genres', () => {
    expect(GENRE_IDS['action']).toBe(28);
    expect(GENRE_IDS['comedy']).toBe(35);
    expect(GENRE_IDS['drama']).toBe(18);
    expect(GENRE_IDS['horror']).toBe(27);
  });

  it('should have 18 genres', () => {
    expect(Object.keys(GENRE_IDS)).toHaveLength(18);
  });
});
