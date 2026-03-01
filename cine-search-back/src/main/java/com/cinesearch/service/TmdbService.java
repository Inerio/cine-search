package com.cinesearch.service;

import com.cinesearch.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Proxy service for the TMDB (The Movie Database) API.
 * Provides movie search, trending/popular listings, person lookup,
 * genre discovery, and advanced filtering with caching support.
 * All methods accept a language parameter (e.g. "fr-FR", "en-US").
 */
@Service
public class TmdbService {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public TmdbService(@Qualifier("tmdbWebClient") WebClient webClient,
                       @Value("${tmdb.api.key}") String apiKey,
                       ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "trending", key = "#lang + '-' + #page")
    public MovieListResponse getTrending(int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trending/movie/week")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    @Cacheable(value = "popular", key = "#lang + '-' + #page")
    public MovieListResponse getPopular(int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieListResponse searchMovies(String query, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieDetailDto getMovieDetail(Long movieId, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("append_to_response", "credits")
                        .build(movieId))
                .retrieve()
                .bodyToMono(MovieDetailDto.class)
                .block();
    }

    /** Returns the current most popular persons from TMDB. */
    @Cacheable(value = "popularPersons", key = "#lang + '-' + #page")
    public PersonSearchResponse getPopularPersons(int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PersonSearchResponse.class)
                .block();
    }

    /** Returns currently trending persons (actors in trending movies this week). */
    @Cacheable(value = "trendingPersons", key = "#lang + '-' + #page")
    public PersonSearchResponse getTrendingPersons(int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trending/person/week")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PersonSearchResponse.class)
                .block();
    }

    /** Searches persons by name on TMDB. */
    public PersonSearchResponse searchPersons(String query, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/person")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PersonSearchResponse.class)
                .block();
    }

    /** Returns person details from TMDB by ID. */
    public PersonDto getPersonDetails(Long personId, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .build(personId))
                .retrieve()
                .bodyToMono(PersonDto.class)
                .block();
    }

    public PersonCreditsResponse getPersonMovies(Long personId, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}/movie_credits")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .build(personId))
                .retrieve()
                .bodyToMono(PersonCreditsResponse.class)
                .block();
    }

    public PersonCreditsResponse getPersonTvShows(Long personId, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}/tv_credits")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .build(personId))
                .retrieve()
                .bodyToMono(PersonCreditsResponse.class)
                .block();
    }

    /** Backward-compatible discover used by AiController. */
    public MovieListResponse discoverMovies(Integer genreId, Integer year, Double minRating, int page, String lang) {
        return discoverMoviesAdvanced(genreId, year, minRating, null, null, null, null, null, null, null, page, lang);
    }

    /** Backward-compatible overload used by AiController for LLM-driven search. */
    public MovieListResponse discoverMoviesAdvanced(Integer genreId, Integer year, Double minRating,
                                                     String originalLanguage, String sortBy, int page, String lang) {
        return discoverMoviesAdvanced(genreId, year, minRating, originalLanguage, sortBy,
                null, null, null, null, null, page, lang);
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
                                                     int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", lang)
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

    /** Returns movies similar to a given movie (TMDB /movie/{id}/similar). */
    public MovieListResponse getSimilarMovies(Long movieId, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}/similar")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build(movieId))
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    /**
     * Discover with comma-separated genre IDs (e.g. "28,12,878") for multi-genre AI search.
     */
    public MovieListResponse discoverMoviesWithGenreIds(String genreIds, Integer year, Double minRating,
                                                         String originalLanguage, String sortBy,
                                                         int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", lang)
                            .queryParam("sort_by", sortBy != null ? sortBy : "popularity.desc")
                            .queryParam("page", page);
                    if (genreIds != null && !genreIds.isBlank()) {
                        uriBuilder.queryParam("with_genres", genreIds);
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

    @Cacheable(value = "genres", key = "#lang")
    public GenreListResponse getGenres(String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/movie/list")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .build())
                .retrieve()
                .bodyToMono(GenreListResponse.class)
                .block();
    }

    /** Returns watch providers (streaming/rent/buy) for a movie in the given region. */
    @Cacheable(value = "watchProviders", key = "#movieId + '-' + #lang")
    public WatchProvidersResponse getWatchProviders(Long movieId, String lang) {
        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/movie/{id}/watch/providers")
                            .queryParam("api_key", apiKey)
                            .build(movieId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null) {
                return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
            }

            String region = extractRegion(lang);
            JsonNode regionNode = root.path("results").path(region);

            if (regionNode.isMissingNode()) {
                return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
            }

            String link = regionNode.has("link") ? regionNode.get("link").asText() : null;
            List<WatchProvidersResponse.ProviderDto> flatrate = parseProviders(regionNode.path("flatrate"));
            List<WatchProvidersResponse.ProviderDto> rent = parseProviders(regionNode.path("rent"));
            List<WatchProvidersResponse.ProviderDto> buy = parseProviders(regionNode.path("buy"));
            return new WatchProvidersResponse(link, flatrate, rent, buy);
        } catch (Exception e) {
            return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
        }
    }

    private List<WatchProvidersResponse.ProviderDto> parseProviders(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isArray()) return List.of();
        try {
            return objectMapper.convertValue(node,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WatchProvidersResponse.ProviderDto.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    // ─── TV SHOW METHODS ──────────────────────────────────────────────────

    @Cacheable(value = "trendingTv", key = "#lang + '-' + #page")
    public MovieListResponse getTrendingTv(int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trending/tv/week")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieListResponse searchTv(String query, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public MovieListResponse searchMulti(String query, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/multi")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    public TvDetailDto getTvDetail(Long tvId, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("append_to_response", "credits")
                        .build(tvId))
                .retrieve()
                .bodyToMono(TvDetailDto.class)
                .block();
    }

    /**
     * Full discover endpoint for TV shows with all TMDB filters.
     * Note: no directorId support (TMDB doesn't support with_crew for TV).
     * Uses first_air_date instead of primary_release_date.
     */
    public MovieListResponse discoverTvAdvanced(Integer genreId, Double minRating,
                                                  String originalLanguage, String sortBy,
                                                  Integer runtimeGte, Integer runtimeLte,
                                                  String decadeStart, String decadeEnd,
                                                  int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/tv")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", lang)
                            .queryParam("sort_by", sortBy != null ? sortBy : "popularity.desc")
                            .queryParam("page", page);
                    if (genreId != null) {
                        uriBuilder.queryParam("with_genres", genreId);
                    }
                    if (decadeStart != null && decadeEnd != null) {
                        uriBuilder.queryParam("first_air_date.gte", decadeStart);
                        uriBuilder.queryParam("first_air_date.lte", decadeEnd);
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
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    /** Discover TV with comma-separated genre IDs for AI search. */
    public MovieListResponse discoverTvWithGenreIds(String genreIds, Integer year, Double minRating,
                                                      String originalLanguage, String sortBy,
                                                      int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/tv")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", lang)
                            .queryParam("sort_by", sortBy != null ? sortBy : "popularity.desc")
                            .queryParam("page", page);
                    if (genreIds != null && !genreIds.isBlank()) {
                        uriBuilder.queryParam("with_genres", genreIds);
                    }
                    if (year != null) {
                        uriBuilder.queryParam("first_air_date_year", year);
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

    public MovieListResponse getSimilarTv(Long tvId, int page, String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/similar")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .queryParam("page", page)
                        .build(tvId))
                .retrieve()
                .bodyToMono(MovieListResponse.class)
                .block();
    }

    @Cacheable(value = "tvGenres", key = "#lang")
    public GenreListResponse getTvGenres(String lang) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/tv/list")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", lang)
                        .build())
                .retrieve()
                .bodyToMono(GenreListResponse.class)
                .block();
    }

    @Cacheable(value = "tvWatchProviders", key = "#tvId + '-' + #lang")
    public WatchProvidersResponse getTvWatchProviders(Long tvId, String lang) {
        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tv/{id}/watch/providers")
                            .queryParam("api_key", apiKey)
                            .build(tvId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null) {
                return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
            }

            String region = extractRegion(lang);
            JsonNode regionNode = root.path("results").path(region);

            if (regionNode.isMissingNode()) {
                return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
            }

            String link = regionNode.has("link") ? regionNode.get("link").asText() : null;
            List<WatchProvidersResponse.ProviderDto> flatrate = parseProviders(regionNode.path("flatrate"));
            List<WatchProvidersResponse.ProviderDto> rent = parseProviders(regionNode.path("rent"));
            List<WatchProvidersResponse.ProviderDto> buy = parseProviders(regionNode.path("buy"));
            return new WatchProvidersResponse(link, flatrate, rent, buy);
        } catch (Exception e) {
            return new WatchProvidersResponse(null, List.of(), List.of(), List.of());
        }
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────

    private String extractRegion(String lang) {
        if (lang != null && lang.contains("-")) {
            return lang.substring(lang.indexOf('-') + 1).toUpperCase();
        }
        if ("fr".equalsIgnoreCase(lang)) return "FR";
        if ("en".equalsIgnoreCase(lang)) return "US";
        return "FR";
    }
}
