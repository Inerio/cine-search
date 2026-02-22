package com.cinesearch.service;

import com.cinesearch.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Proxy service for the TMDB (The Movie Database) API.
 * Provides movie search, trending/popular listings, person lookup,
 * genre discovery, and advanced filtering with caching support.
 */
@Service
public class TmdbService {

    private final WebClient webClient;
    private final String apiKey;

    public TmdbService(@Qualifier("tmdbWebClient") WebClient webClient,
                       @Value("${tmdb.api.key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    @Cacheable(value = "trending", key = "#page")
    public MovieListResponse getTrending(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trending/movie/week")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    @Cacheable(value = "popular", key = "#page")
    public MovieListResponse getPopular(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieListResponse searchMovies(String query, int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieDetailDto getMovieDetail(Long movieId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("append_to_response", "credits")
                        .build(movieId))
                .retrieve()
                .bodyToMono(MovieDetailDto.class)
                .block();
    }

    /** Returns the current most popular persons from TMDB. */
    @Cacheable(value = "popularPersons", key = "#page")
    public PersonSearchResponse getPopularPersons(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PersonSearchResponse.class)
                .block();
    }

    /** Searches persons by name on TMDB. */
    public PersonSearchResponse searchPersons(String query, int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/person")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PersonSearchResponse.class)
                .block();
    }

    public PersonCreditsResponse getPersonMovies(Long personId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}/movie_credits")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .build(personId))
                .retrieve()
                .bodyToMono(PersonCreditsResponse.class)
                .block();
    }

    /** Backward-compatible discover used by AiController. */
    public MovieListResponse discoverMovies(Integer genreId, Integer year, Double minRating, int page) {
        return discoverMoviesAdvanced(genreId, year, minRating, null, null, null, null, null, null, null, page);
    }

    /** Backward-compatible overload used by AiController for LLM-driven search. */
    public MovieListResponse discoverMoviesAdvanced(Integer genreId, Integer year, Double minRating,
                                                     String originalLanguage, String sortBy, int page) {
        return discoverMoviesAdvanced(genreId, year, minRating, originalLanguage, sortBy,
                null, null, null, null, null, page);
    }

    /**
     * Full discover endpoint with all TMDB filters:
     * genre, year/decade, rating, language, sort, runtime, and director (crew).
     */
    public MovieListResponse discoverMoviesAdvanced(Integer genreId, Integer year, Double minRating,
                                                     String originalLanguage, String sortBy,
                                                     Integer runtimeGte, Integer runtimeLte,
                                                     Long directorId,
                                                     String decadeStart, String decadeEnd,
                                                     int page) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", "fr-FR")
                            .queryParam("sort_by", sortBy != null ? sortBy : "popularity.desc")
                            .queryParam("page", page);
                    if (genreId != null) {
                        uriBuilder.queryParam("with_genres", genreId);
                    }
                    // Decade date range takes priority over exact year
                    if (decadeStart != null && decadeEnd != null) {
                        uriBuilder.queryParam("primary_release_date.gte", decadeStart);
                        uriBuilder.queryParam("primary_release_date.lte", decadeEnd);
                    } else if (year != null) {
                        uriBuilder.queryParam("primary_release_year", year);
                    }
                    if (minRating != null) {
                        uriBuilder.queryParam("vote_average.gte", minRating);
                        uriBuilder.queryParam("vote_count.gte", 50);
                    }
                    if (originalLanguage != null) {
                        uriBuilder.queryParam("with_original_language", originalLanguage);
                    }
                    if (runtimeGte != null) {
                        uriBuilder.queryParam("with_runtime.gte", runtimeGte);
                    }
                    if (runtimeLte != null) {
                        uriBuilder.queryParam("with_runtime.lte", runtimeLte);
                    }
                    if (directorId != null) {
                        uriBuilder.queryParam("with_crew", directorId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    @Cacheable("genres")
    public GenreListResponse getGenres() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/movie/list")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "fr-FR")
                        .build())
                .retrieve()
                .bodyToMono(GenreListResponse.class)
                .block();
    }
}
