package com.cinesearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.cinesearch.dto.GenreListResponse;
import com.cinesearch.dto.MovieDto;
import com.cinesearch.dto.MovieListResponse;
import com.cinesearch.dto.PersonCreditsResponse;
import com.cinesearch.dto.PersonDto;
import com.cinesearch.dto.PersonSearchResponse;
import com.cinesearch.dto.WatchProvidersResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link TmdbService}.
 * Uses Mockito deep stubs to mock the WebClient reactive chain
 * without requiring a running Spring context.
 */
@ExtendWith(MockitoExtension.class)
class TmdbServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TmdbService tmdbService;

    @BeforeEach
    void setUp() {
        tmdbService = new TmdbService(webClient, "test-tmdb-api-key", objectMapper);
    }

    // ─── searchMovies ───────────────────────────────────────────────────────

    @Test
    void searchMovies_shouldReturnParsedMovieListResponse() {
        MovieListResponse expected = new MovieListResponse(
                1,
                List.of(new MovieDto(550L, "Fight Club", "An insomniac office worker...",
                        "/poster.jpg", "/backdrop.jpg", "1999-10-15",
                        8.4, 25000, 63.5, List.of(18, 53), "en", null, null)),
                5,
                100
        );

        stubGetRequest(MovieListResponse.class, expected);

        MovieListResponse result = tmdbService.searchMovies("Fight Club", 1, "en-US");

        assertNotNull(result);
        assertEquals(1, result.page());
        assertEquals(5, result.total_pages());
        assertEquals(100, result.total_results());
        assertEquals(1, result.results().size());
        assertEquals("Fight Club", result.results().getFirst().title());
        assertEquals(550L, result.results().getFirst().id());
    }

    // ─── searchTv ───────────────────────────────────────────────────────────

    @Test
    void searchTv_shouldReturnParsedMovieListResponse() {
        MovieListResponse expected = new MovieListResponse(
                1,
                List.of(new MovieDto(1399L, "Breaking Bad", "A high school chemistry teacher...",
                        "/bb_poster.jpg", "/bb_backdrop.jpg", "2008-01-20",
                        8.9, 12000, 95.0, List.of(18, 80), "en", null, null)),
                3,
                50
        );

        stubGetRequest(MovieListResponse.class, expected);

        MovieListResponse result = tmdbService.searchTv("Breaking Bad", 1, "en-US");

        assertNotNull(result);
        assertEquals(1, result.page());
        assertEquals(1, result.results().size());
        assertEquals("Breaking Bad", result.results().getFirst().title());
        assertEquals(1399L, result.results().getFirst().id());
    }

    // ─── getTrending ────────────────────────────────────────────────────────

    @Test
    void getTrending_shouldReturnResults() {
        MovieListResponse expected = new MovieListResponse(
                1,
                List.of(
                        new MovieDto(299536L, "Avengers: Infinity War", "The Avengers...",
                                "/avengers_poster.jpg", "/avengers_backdrop.jpg", "2018-04-25",
                                8.3, 30000, 120.0, List.of(12, 28, 878), "en", null, null),
                        new MovieDto(157336L, "Interstellar", "A team of explorers...",
                                "/interstellar_poster.jpg", null, "2014-11-05",
                                8.6, 28000, 85.0, List.of(12, 18, 878), "en", null, null)
                ),
                10,
                200
        );

        stubGetRequest(MovieListResponse.class, expected);

        MovieListResponse result = tmdbService.getTrending(1, "fr-FR");

        assertNotNull(result);
        assertEquals(2, result.results().size());
        assertEquals(10, result.total_pages());
        assertEquals(200, result.total_results());
    }

    // ─── getGenres ──────────────────────────────────────────────────────────

    @Test
    void getGenres_shouldReturnGenreListResponse() {
        GenreListResponse expected = new GenreListResponse(List.of(
                new GenreListResponse.Genre(28, "Action"),
                new GenreListResponse.Genre(35, "Comedy"),
                new GenreListResponse.Genre(18, "Drama"),
                new GenreListResponse.Genre(27, "Horror")
        ));

        stubGetRequest(GenreListResponse.class, expected);

        GenreListResponse result = tmdbService.getGenres("en-US");

        assertNotNull(result);
        assertEquals(4, result.genres().size());
        assertEquals("Action", result.genres().getFirst().name());
        assertEquals(28, result.genres().getFirst().id());
    }

    // ─── getWatchProviders ──────────────────────────────────────────────────

    @Test
    void getWatchProviders_shouldReturnProvidersForCorrectRegion() throws Exception {
        // Build a realistic TMDB watch providers JSON response
        String watchProvidersJson = """
                {
                  "id": 550,
                  "results": {
                    "FR": {
                      "link": "https://www.themoviedb.org/movie/550/watch?locale=FR",
                      "flatrate": [
                        {"provider_id": 8, "provider_name": "Netflix", "logo_path": "/netflix.jpg"}
                      ],
                      "rent": [
                        {"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/apple.jpg"}
                      ],
                      "buy": [
                        {"provider_id": 3, "provider_name": "Google Play", "logo_path": "/google.jpg"}
                      ]
                    },
                    "US": {
                      "link": "https://www.themoviedb.org/movie/550/watch?locale=US",
                      "flatrate": [
                        {"provider_id": 337, "provider_name": "Disney Plus", "logo_path": "/disney.jpg"}
                      ],
                      "rent": [],
                      "buy": []
                    }
                  }
                }
                """;

        JsonNode jsonNode = objectMapper.readTree(watchProvidersJson);
        stubGetRequest(JsonNode.class, jsonNode);

        // lang "fr-FR" should extract region "FR"
        WatchProvidersResponse result = tmdbService.getWatchProviders(550L, "fr-FR");

        assertNotNull(result);
        assertEquals("https://www.themoviedb.org/movie/550/watch?locale=FR", result.link());
        assertEquals(1, result.flatrate().size());
        assertEquals("Netflix", result.flatrate().getFirst().provider_name());
        assertEquals(8, result.flatrate().getFirst().provider_id());
        assertEquals(1, result.rent().size());
        assertEquals("Apple TV", result.rent().getFirst().provider_name());
        assertEquals(1, result.buy().size());
        assertEquals("Google Play", result.buy().getFirst().provider_name());
    }

    // ─── getTvWatchProviders ────────────────────────────────────────────────

    @Test
    void getTvWatchProviders_shouldReturnProvidersViSharedHelper() throws Exception {
        String watchProvidersJson = """
                {
                  "id": 1399,
                  "results": {
                    "US": {
                      "link": "https://www.themoviedb.org/tv/1399/watch?locale=US",
                      "flatrate": [
                        {"provider_id": 384, "provider_name": "HBO Max", "logo_path": "/hbo.jpg"}
                      ],
                      "rent": [],
                      "buy": [
                        {"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/apple.jpg"}
                      ]
                    }
                  }
                }
                """;

        JsonNode jsonNode = objectMapper.readTree(watchProvidersJson);
        stubGetRequest(JsonNode.class, jsonNode);

        WatchProvidersResponse result = tmdbService.getTvWatchProviders(1399L, "en-US");

        assertNotNull(result);
        assertEquals("https://www.themoviedb.org/tv/1399/watch?locale=US", result.link());
        assertEquals(1, result.flatrate().size());
        assertEquals("HBO Max", result.flatrate().getFirst().provider_name());
        assertTrue(result.rent().isEmpty());
        assertEquals(1, result.buy().size());
    }

    // ─── getWatchProviders null API response ────────────────────────────────

    @Test
    void getWatchProviders_whenApiReturnsNull_shouldReturnEmptyProviders() {
        stubGetRequest(JsonNode.class, null);

        WatchProvidersResponse result = tmdbService.getWatchProviders(999L, "fr-FR");

        assertNotNull(result);
        assertNull(result.link());
        assertTrue(result.flatrate().isEmpty());
        assertTrue(result.rent().isEmpty());
        assertTrue(result.buy().isEmpty());
    }

    // ─── searchPersons ──────────────────────────────────────────────────────

    @Test
    void searchPersons_shouldReturnPersonSearchResponse() {
        PersonSearchResponse expected = new PersonSearchResponse(
                1,
                List.of(new PersonDto(
                        6193L, "Leonardo DiCaprio", "/leo.jpg",
                        "Acting", 80.5, 2,
                        List.of(new MovieDto(27205L, "Inception", "A thief...",
                                "/inception.jpg", null, "2010-07-16",
                                8.4, 33000, 100.0, List.of(28, 878, 12), "en", null, null))
                )),
                1,
                1
        );

        stubGetRequest(PersonSearchResponse.class, expected);

        PersonSearchResponse result = tmdbService.searchPersons("Leonardo DiCaprio", 1, "en-US");

        assertNotNull(result);
        assertEquals(1, result.results().size());
        assertEquals("Leonardo DiCaprio", result.results().getFirst().name());
        assertEquals(6193L, result.results().getFirst().id());
        assertEquals("Acting", result.results().getFirst().known_for_department());
    }

    // ─── getPersonMovies ────────────────────────────────────────────────────

    @Test
    void getPersonMovies_shouldReturnPersonCreditsResponse() {
        PersonCreditsResponse expected = new PersonCreditsResponse(
                6193L,
                List.of(
                        new MovieDto(27205L, "Inception", "A thief...",
                                "/inception.jpg", "/inception_bg.jpg", "2010-07-16",
                                8.4, 33000, 100.0, List.of(28, 878), "en", null, null),
                        new MovieDto(11324L, "Shutter Island", "In 1954...",
                                "/shutter.jpg", null, "2010-02-14",
                                8.2, 20000, 70.0, List.of(18, 53, 9648), "en", null, null)
                ),
                List.of(
                        new MovieDto(1930L, "The Aviator", "A biopic...",
                                "/aviator.jpg", null, "2004-12-25",
                                7.5, 8000, 40.0, List.of(18), "en", null, null)
                )
        );

        stubGetRequest(PersonCreditsResponse.class, expected);

        PersonCreditsResponse result = tmdbService.getPersonMovies(6193L, "en-US");

        assertNotNull(result);
        assertEquals(6193L, result.id());
        assertEquals(2, result.cast().size());
        assertEquals("Inception", result.cast().getFirst().title());
        assertEquals(1, result.crew().size());
        assertEquals("The Aviator", result.crew().getFirst().title());
    }

    // ─── extractRegion ──────────────────────────────────────────────────────

    @Test
    void extractRegion_frFR_shouldReturnFR() {
        String region = invokeExtractRegion("fr-FR");
        assertEquals("FR", region);
    }

    @Test
    void extractRegion_enUS_shouldReturnUS() {
        String region = invokeExtractRegion("en-US");
        assertEquals("US", region);
    }

    @Test
    void extractRegion_plainFr_shouldReturnFR() {
        String region = invokeExtractRegion("fr");
        assertEquals("FR", region);
    }

    @Test
    void extractRegion_plainEn_shouldReturnUS() {
        String region = invokeExtractRegion("en");
        assertEquals("US", region);
    }

    @Test
    void extractRegion_null_shouldDefaultToFR() {
        String region = invokeExtractRegion(null);
        assertEquals("FR", region);
    }

    // ─── Helper methods ─────────────────────────────────────────────────────

    /**
     * Stubs the WebClient GET chain to return the given value for any URI.
     * Uses the deep-stubs mock so that {@code webClient.get().uri(...).retrieve().bodyToMono(...).block()}
     * resolves to the provided response object.
     */
    @SuppressWarnings("unchecked")
    private <T> void stubGetRequest(Class<T> responseType, T response) {
        when(webClient.get()
                .uri(any(java.util.function.Function.class))
                .retrieve()
                .bodyToMono(responseType)
                .block()
        ).thenReturn(response);
    }

    /**
     * Invokes the private {@code extractRegion} method via reflection.
     */
    private String invokeExtractRegion(String lang) {
        return (String) ReflectionTestUtils.invokeMethod(tmdbService, "extractRegion", lang);
    }
}
