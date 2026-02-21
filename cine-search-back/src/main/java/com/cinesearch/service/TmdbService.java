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

    public MovieListResponse discoverMovies(Integer genreId, Integer year, Double minRating, int page) {
        return discoverMoviesAdvanced(genreId, year, minRating, null, null, page);
    }

    /**
     * Advanced discover endpoint with language and sort support.
     * Used by AiController for LLM-driven search with filters.
     */
    public MovieListResponse discoverMoviesAdvanced(Integer genreId, Integer year, Double minRating,
                                                     String originalLanguage, String sortBy, int page) {
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
                    if (year != null) {
                        uriBuilder.queryParam("primary_release_year", year);
                    }
                    if (minRating != null) {
                        uriBuilder.queryParam("vote_average.gte", minRating);
                        uriBuilder.queryParam("vote_count.gte", 50);
                    }
                    if (originalLanguage != null) {
                        uriBuilder.queryParam("with_original_language", originalLanguage);
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
