package com.cinesearch.controller;

import com.cinesearch.dto.*;
import com.cinesearch.service.GroqService;
import com.cinesearch.service.TmdbService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroqService groqService;

    @MockitoBean
    private TmdbService tmdbService;

    // --- Shared test data ---

    private static final MovieDto TITANIC = new MovieDto(
            597L, "Titanic", "84 years later...",
            "/poster.jpg", "/backdrop.jpg", "1997-12-19",
            7.9, 22000, 100.0, List.of(18, 10749), "en", null, "movie");

    private static final MovieDto BREAKING_BAD = new MovieDto(
            1396L, "Breaking Bad", "A high school chemistry teacher...",
            "/bb.jpg", "/bb_bg.jpg", "2008-01-20",
            8.9, 12000, 95.0, List.of(18, 80), "en", null, "tv");

    private static final MovieListResponse TITANIC_RESULTS = new MovieListResponse(
            1, List.of(TITANIC), 1, 1);

    private static final MovieListResponse BB_RESULTS = new MovieListResponse(
            1, List.of(BREAKING_BAD), 1, 1);

    private static final MovieListResponse EMPTY_RESULTS = new MovieListResponse(
            1, List.of(), 1, 0);

    // --- Tests ---

    @Test
    @DisplayName("POST /api/ai/parse with title finds bestMatch")
    void parse_withTitle_findsBestMatch() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setTitle("Titanic");
        parsed.setYear(1997);
        parsed.setGenres(List.of("drama", "romance"));

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        when(tmdbService.searchMovies(eq("Titanic"), eq(1), anyString())).thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMovies(eq("Titanic"), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(eq(597L), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("find titanic movie", "movie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch").exists())
                .andExpect(jsonPath("$.bestMatch.title").value("Titanic"))
                .andExpect(jsonPath("$.bestMatch.id").value(597))
                .andExpect(jsonPath("$.parsed.intent").value("search"));

        verify(groqService).parseUserQuery("find titanic movie");
    }

    @Test
    @DisplayName("Unknown intent returns empty results")
    void parse_unknownIntent_returnsEmpty() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("unknown");

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("gibberish", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch").doesNotExist())
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.totalResults").value(0));
    }

    @Test
    @DisplayName("Falls back to alternate titles when title search fails")
    void parse_fallsToAlternateTitles() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setTitle("Le Titanic");
        parsed.setAlternateTitles(List.of("Titanic"));

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        // Title search fails
        when(tmdbService.searchMovies(eq("Le Titanic"), eq(1), anyString())).thenReturn(EMPTY_RESULTS);
        // Alternate title succeeds
        when(tmdbService.searchMovies(eq("Titanic"), eq(1), anyString())).thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMovies(eq("Titanic"), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(eq(597L), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("le titanic film", "movie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch.title").value("Titanic"));
    }

    @Test
    @DisplayName("Falls back to search queries when title and alternates fail")
    void parse_fallsToSearchQueries() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setSearchQueries(List.of("Loulou Montmartre", "Loulou orpheline Paris"));

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        // search query "Loulou Montmartre" fails
        when(tmdbService.searchMulti(eq("Loulou Montmartre"), eq(1), anyString())).thenReturn(EMPTY_RESULTS);
        // search query "Loulou orpheline Paris" succeeds
        MovieDto loulou = new MovieDto(99999L, "Loulou de Montmartre", "...",
                null, null, "2006-01-01", 6.5, 100, 10.0, List.of(16), "fr", null, null);
        MovieListResponse loulouResults = new MovieListResponse(1, List.of(loulou), 1, 1);
        when(tmdbService.searchMulti(eq("Loulou orpheline Paris"), eq(1), anyString())).thenReturn(loulouResults);
        when(tmdbService.getSimilarMovies(anyLong(), eq(1), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarTv(anyLong(), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("une petite orpheline à montmartre", "all"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch").exists())
                .andExpect(jsonPath("$.bestMatch.id").value(99999));
    }

    @Test
    @DisplayName("Actor search finds movie via person credits")
    void parse_actorSearch_findsMovie() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setActors(List.of("Leonardo DiCaprio"));
        parsed.setYear(1997);
        parsed.setGenres(List.of("drama"));

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        // All earlier steps fail (no title, no alternates, no search queries, no query)
        PersonSearchResponse personSearch = new PersonSearchResponse(1,
                List.of(new PersonDto(6193L, "Leonardo DiCaprio", "/leo.jpg", "Acting", 80.0, 2, null)),
                1, 1);
        when(tmdbService.searchPersons(eq("Leonardo DiCaprio"), eq(1), anyString())).thenReturn(personSearch);
        PersonCreditsResponse credits = new PersonCreditsResponse(6193L,
                List.of(TITANIC), List.of());
        when(tmdbService.getPersonMovies(eq(6193L), anyString())).thenReturn(credits);
        when(tmdbService.getPersonTvShows(eq(6193L), anyString())).thenReturn(new PersonCreditsResponse(6193L, List.of(), List.of()));
        when(tmdbService.searchMulti(anyString(), eq(1), anyString())).thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMulti(anyString(), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(eq(597L), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("DiCaprio boat movie 1997", "all"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch").exists())
                .andExpect(jsonPath("$.bestMatch.title").value("Titanic"));
    }

    @Test
    @DisplayName("Keyword search finds movie when all else fails")
    void parse_keywordSearch_findsMovie() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setKeywords(List.of("iceberg", "ship", "ocean", "love", "1912"));

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        // combined keywords search succeeds
        when(tmdbService.searchMulti(eq("iceberg ship ocean love 1912"), eq(1), anyString()))
                .thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMulti(eq("iceberg ship ocean love 1912"), eq(2), anyString()))
                .thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(anyLong(), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("a ship that sinks after hitting ice", "all"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch").exists());
    }

    @Test
    @DisplayName("Raw text fallback returns results when all steps fail")
    void parse_rawTextFallback() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        // Everything fails until raw text
        when(tmdbService.searchMulti(anyString(), eq(1), anyString())).thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMulti(anyString(), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(anyLong(), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("some movie about ships", "all"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("When bestMatch found, similar movies are fetched")
    void parse_bestMatch_fetchesSimilar() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setTitle("Titanic");

        MovieDto similar1 = new MovieDto(1L, "The Notebook", "...", null, null, "2004-06-25",
                7.8, 10000, 50.0, List.of(18, 10749), "en", null, "movie");
        MovieListResponse similarResponse = new MovieListResponse(1, List.of(similar1), 1, 1);

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        when(tmdbService.searchMovies(eq("Titanic"), eq(1), anyString())).thenReturn(TITANIC_RESULTS);
        when(tmdbService.searchMovies(eq("Titanic"), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarMovies(eq(597L), eq(1), anyString())).thenReturn(similarResponse);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("titanic", "movie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch.title").value("Titanic"))
                .andExpect(jsonPath("$.similarMovies").isArray())
                .andExpect(jsonPath("$.similarMovies[0].title").value("The Notebook"));

        verify(tmdbService).getSimilarMovies(597L, 1, "fr-FR");
    }

    @Test
    @DisplayName("mediaType=tv searches TV only")
    void parse_tvMediaType_searchesTv() throws Exception {
        AiMovieQuery parsed = new AiMovieQuery();
        parsed.setIntent("search");
        parsed.setTitle("Breaking Bad");
        parsed.setType("tv");

        when(groqService.parseUserQuery(anyString())).thenReturn(parsed);
        when(tmdbService.searchTv(eq("Breaking Bad"), eq(1), anyString())).thenReturn(BB_RESULTS);
        when(tmdbService.searchTv(eq("Breaking Bad"), eq(2), anyString())).thenReturn(EMPTY_RESULTS);
        when(tmdbService.getSimilarTv(eq(1396L), eq(1), anyString())).thenReturn(EMPTY_RESULTS);

        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("lang", "fr-FR")
                        .content(objectMapper.writeValueAsString(new AiParseRequest("breaking bad serie", "tv"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMatch.title").value("Breaking Bad"));

        verify(tmdbService).searchTv(eq("Breaking Bad"), eq(1), anyString());
        verify(tmdbService, never()).searchMovies(eq("Breaking Bad"), anyInt(), anyString());
    }

    @Test
    @DisplayName("Invalid request (empty text) returns 400")
    void parse_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/ai/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiParseRequest("", null))))
                .andExpect(status().isBadRequest());
    }
}
