import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  MovieListResponse,
  MovieDetail,
  GenreListResponse,
  PersonSearchResponse,
  PersonCreditsResponse,
  AiSearchResponse
} from '../models/movie.model';

/** Central HTTP service for all TMDB and AI endpoints. */
@Injectable({ providedIn: 'root' })
export class MovieService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // --- Movies ---

  getTrending(page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/trending`, {
      params: new HttpParams().set('page', page)
    });
  }

  getPopular(page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/popular`, {
      params: new HttpParams().set('page', page)
    });
  }

  searchMovies(query: string, page = 1): Observable<MovieListResponse> {
    return this.http.get<MovieListResponse>(`${this.apiUrl}/movies/search`, {
      params: new HttpParams().set('query', query).set('page', page)
    });
  }

  getMovieDetail(id: number): Observable<MovieDetail> {
    return this.http.get<MovieDetail>(`${this.apiUrl}/movies/${id}`);
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
    let params = new HttpParams().set('page', filters.page ?? 1);
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

  getGenres(): Observable<GenreListResponse> {
    return this.http.get<GenreListResponse>(`${this.apiUrl}/movies/genres`);
  }

  // --- Persons ---

  getPopularActors(page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/popular`, {
      params: new HttpParams().set('page', page)
    });
  }

  /** Returns currently trending persons (actors appearing in trending movies this week). */
  getTrendingActors(page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/trending`, {
      params: new HttpParams().set('page', page)
    });
  }

  searchPersons(query: string, page = 1): Observable<PersonSearchResponse> {
    return this.http.get<PersonSearchResponse>(`${this.apiUrl}/persons/search`, {
      params: new HttpParams().set('query', query).set('page', page)
    });
  }

  getPersonMovies(personId: number): Observable<PersonCreditsResponse> {
    return this.http.get<PersonCreditsResponse>(`${this.apiUrl}/persons/${personId}/movies`);
  }

  // --- AI ---

  /** Sends free-text query to backend Groq LLM for structured extraction + TMDB resolution. */
  aiParse(text: string): Observable<AiSearchResponse> {
    return this.http.post<AiSearchResponse>(`${this.apiUrl}/ai/parse`, { text });
  }
}
