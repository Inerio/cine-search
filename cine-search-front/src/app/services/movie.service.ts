import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { TranslationService } from './translation.service';
import {
  MovieListResponse,
  MovieDetail,
  GenreListResponse,
  Person,
  PersonSearchResponse,
  PersonCreditsResponse,
  WatchProviders,
  AiSearchResponse,
  TvDetail
} from '../models/movie.model';

/** Central HTTP service for all TMDB and AI endpoints. */
@Injectable({ providedIn: 'root' })
export class MovieService {
  private http = inject(HttpClient);
  private ts = inject(TranslationService);
  private apiUrl = environment.apiUrl;

  /** Returns the current TMDB locale (e.g. "fr-FR" or "en-US"). */
  private get lang(): string {
    return this.ts.locale();
  }

  // --- Movies ---

  getTrending(page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/trending`, {
      params: new HttpParams().set('page', page).set('lang', this.lang)
    });
  }

  getPopular(page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/popular`, {
      params: new HttpParams().set('page', page).set('lang', this.lang)
    });
  }

  searchMovies(query: string, page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/search`, {
      params: new HttpParams().set('query', query).set('page', page).set('lang', this.lang)
    });
  }

  getMovieDetail(id: number): Observable<MovieDetail> {
    return this.http.get<MovieDetail>(`${this.apiUrl}/movies/${id}`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  /** Discover movies with multi-filter support (genre, decade, rating, etc.). */
  discoverMovies(filters: {
    genreId?: number;
    year?: number;
    minRating?: number;
    language?: string;
    sortBy?: string;
    runtimeGte?: number;
    runtimeLte?: number;
    directorId?: number;
    decadeStart?: string;
    decadeEnd?: string;
    page?: number;
  }): Observable<MovieListResponse> {
    let params = new HttpParams()
      .set('page', filters.page ?? 1)
      .set('lang', this.lang);
    if (filters.genreId) params = params.set('genreId', filters.genreId);
    if (filters.year) params = params.set('year', filters.year);
    if (filters.minRating) params = params.set('minRating', filters.minRating);
    if (filters.language) params = params.set('language', filters.language);
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.runtimeGte) params = params.set('runtimeGte', filters.runtimeGte);
    if (filters.runtimeLte) params = params.set('runtimeLte', filters.runtimeLte);
    if (filters.directorId) params = params.set('directorId', filters.directorId);
    if (filters.decadeStart) params = params.set('decadeStart', filters.decadeStart);
    if (filters.decadeEnd) params = params.set('decadeEnd', filters.decadeEnd);
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/discover`, { params });
  }

  getWatchProviders(movieId: number): Observable<WatchProviders> {
    return this.http.get<WatchProviders>(`${this.apiUrl}/movies/${movieId}/watch-providers`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  getGenres(): Observable<GenreListResponse> {
    return this.http.get<GenreListResponse>(`${this.apiUrl}/movies/genres`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  // --- Persons ---

  getPopularActors(page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/popular`, {
      params: new HttpParams().set('page', page).set('lang', this.lang)
    });
  }

  /** Returns currently trending persons (actors appearing in trending movies this week). */
  getTrendingActors(page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/trending`, {
      params: new HttpParams().set('page', page).set('lang', this.lang)
    });
  }

  searchPersons(query: string, page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/search`, {
      params: new HttpParams().set('query', query).set('page', page).set('lang', this.lang)
    });
  }

  /** Returns person details by ID (for auto-select from queryParam). */
  getPersonDetails(personId: number): Observable<Person> {
    return this.http.get<Person>(`${this.apiUrl}/persons/${personId}`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  getPersonMovies(personId: number): Observable<PersonCreditsResponse> {
    return this.http.get<PersonCreditsResponse>(`${this.apiUrl}/persons/${personId}/movies`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  // --- TV Shows ---

  getTrendingTv(page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/tv/trending`, {
      params: new HttpParams().set('page', page).set('lang', this.lang)
    });
  }

  searchTv(query: string, page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/tv/search`, {
      params: new HttpParams().set('query', query).set('page', page).set('lang', this.lang)
    });
  }

  getTvDetail(id: number): Observable<TvDetail> {
    return this.http.get<TvDetail>(`${this.apiUrl}/tv/${id}`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  discoverTv(filters: {
    genreId?: number;
    minRating?: number;
    language?: string;
    sortBy?: string;
    runtimeGte?: number;
    runtimeLte?: number;
    decadeStart?: string;
    decadeEnd?: string;
    page?: number;
  }): Observable<MovieListResponse> {
    let params = new HttpParams()
      .set('page', filters.page ?? 1)
      .set('lang', this.lang);
    if (filters.genreId) params = params.set('genreId', filters.genreId);
    if (filters.minRating) params = params.set('minRating', filters.minRating);
    if (filters.language) params = params.set('language', filters.language);
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.runtimeGte) params = params.set('runtimeGte', filters.runtimeGte);
    if (filters.runtimeLte) params = params.set('runtimeLte', filters.runtimeLte);
    if (filters.decadeStart) params = params.set('decadeStart', filters.decadeStart);
    if (filters.decadeEnd) params = params.set('decadeEnd', filters.decadeEnd);
    return this.http.get<MovieListResponse>(`${this.apiUrl}/tv/discover`, { params });
  }

  getTvGenres(): Observable<GenreListResponse> {
    return this.http.get<GenreListResponse>(`${this.apiUrl}/tv/genres`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  getTvWatchProviders(tvId: number): Observable<WatchProviders> {
    return this.http.get<WatchProviders>(`${this.apiUrl}/tv/${tvId}/watch-providers`, {
      params: new HttpParams().set('lang', this.lang)
    });
  }

  // --- AI ---

  /** Sends free-text query to backend Groq LLM for structured extraction + TMDB resolution. */
  aiParse(text: string, mediaType: string = 'all'): Observable<AiSearchResponse> {
    return this.http.post<AiSearchResponse>(`${this.apiUrl}/ai/parse?lang=${this.lang}`, { text, mediaType });
  }
}
