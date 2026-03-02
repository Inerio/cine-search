package com.cinesearch.controller;

import com.cinesearch.dto.*;
import com.cinesearch.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TvController.class)
class TvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbService tmdbService;

    private MovieListResponse sampleTvList;
    private TvDetailDto sampleTvDetail;
    private GenreListResponse sampleGenreList;
    private WatchProvidersResponse sampleWatchProviders;

    @BeforeEach
    void setUp() {
        MovieDto tvShow = new MovieDto(
                1396L, "Breaking Bad", "A high school chemistry teacher diagnosed with cancer...",
                "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg", "/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                "2008-01-20", 8.9, 12000, 95.0,
                List.of(18, 80), "en", null, "tv"
        );
        sampleTvList = new MovieListResponse(1, List.of(tvShow), 5, 100);

        sampleTvDetail = new TvDetailDto(
                1396L, "Breaking Bad", "A high school chemistry teacher diagnosed with cancer...",
                "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg", "/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                "2008-01-20", "2013-09-29", 8.9, 12000,
                List.of(47), 5, 62, "Remember my name.",
                "Ended",
                List.of(new TvDetailDto.GenreDto(18, "Drama")),
                List.of(new TvDetailDto.SeasonDto(1L, "Season 1", 1, 7, "/1BP4xYv9ZG4ZVHkL7ocOziBbSYH.jpg", "2008-01-20", "Season 1 overview")),
                List.of(new TvDetailDto.NetworkDto(174, "AMC", "/pmvRmATOCaDykE6JrVoeYxlFHw3.jpg")),
                List.of(new TvDetailDto.CreatorDto(66633L, "Vince Gilligan", "/uFh3OrBvkiFIjJJikNsIuAIjKmW.jpg")),
                new TvDetailDto.CreditsDto(
                        List.of(new TvDetailDto.CastMemberDto(17419L, "Bryan Cranston", "Walter White", "/7Jahy5LZX2Fo8fGJltMreAI49hC.jpg", 0)),
                        List.of(new TvDetailDto.CrewMemberDto(66633L, "Vince Gilligan", "Executive Producer", "Production", "/uFh3OrBvkiFIjJJikNsIuAIjKmW.jpg"))
                )
        );

        sampleGenreList = new GenreListResponse(
                List.of(new GenreListResponse.Genre(18, "Drama"), new GenreListResponse.Genre(80, "Crime"))
        );

        sampleWatchProviders = new WatchProvidersResponse(
                "https://www.themoviedb.org/tv/1396/watch",
                List.of(new WatchProvidersResponse.ProviderDto(8, "Netflix", "/t2yyOv40HZeVlLjYsCsPHnWLk4W.jpg")),
                List.of(),
                List.of()
        );
    }

    @Test
    @DisplayName("GET /api/tv/trending returns 200 with TV show list")
    void getTrendingTv_returnsOk() throws Exception {
        when(tmdbService.getTrendingTv(1, "fr-FR")).thenReturn(sampleTvList);

        mockMvc.perform(get("/api/tv/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.results[0].id").value(1396))
                .andExpect(jsonPath("$.results[0].title").value("Breaking Bad"))
                .andExpect(jsonPath("$.total_pages").value(5));

        verify(tmdbService).getTrendingTv(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/tv/trending with custom page and lang")
    void getTrendingTv_withCustomParams() throws Exception {
        when(tmdbService.getTrendingTv(2, "en-US")).thenReturn(sampleTvList);

        mockMvc.perform(get("/api/tv/trending")
                        .param("page", "2")
                        .param("lang", "en-US"))
                .andExpect(status().isOk());

        verify(tmdbService).getTrendingTv(2, "en-US");
    }

    @Test
    @DisplayName("GET /api/tv/search returns 200 with search results")
    void searchTv_returnsOk() throws Exception {
        when(tmdbService.searchTv("Breaking Bad", 1, "fr-FR")).thenReturn(sampleTvList);

        mockMvc.perform(get("/api/tv/search")
                        .param("query", "Breaking Bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].title").value("Breaking Bad"));

        verify(tmdbService).searchTv("Breaking Bad", 1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/tv/search without query returns 500 (missing required param)")
    void searchTv_missingQuery_returnsError() throws Exception {
        mockMvc.perform(get("/api/tv/search"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/tv/{id} returns 200 with TV detail")
    void getTvDetail_returnsOk() throws Exception {
        when(tmdbService.getTvDetail(1396L, "fr-FR")).thenReturn(sampleTvDetail);

        mockMvc.perform(get("/api/tv/1396"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1396))
                .andExpect(jsonPath("$.name").value("Breaking Bad"))
                .andExpect(jsonPath("$.number_of_seasons").value(5))
                .andExpect(jsonPath("$.genres[0].name").value("Drama"))
                .andExpect(jsonPath("$.created_by[0].name").value("Vince Gilligan"));

        verify(tmdbService).getTvDetail(1396L, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/tv/discover returns 200 with discovered TV shows")
    void discoverTv_returnsOk() throws Exception {
        when(tmdbService.discoverTvAdvanced(
                eq(18), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                eq(1), eq("fr-FR")
        )).thenReturn(sampleTvList);

        mockMvc.perform(get("/api/tv/discover")
                        .param("genreId", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());

        verify(tmdbService).discoverTvAdvanced(
                eq(18), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                eq(1), eq("fr-FR")
        );
    }

    @Test
    @DisplayName("GET /api/tv/genres returns 200 with TV genre list")
    void getTvGenres_returnsOk() throws Exception {
        when(tmdbService.getTvGenres("fr-FR")).thenReturn(sampleGenreList);

        mockMvc.perform(get("/api/tv/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").isArray())
                .andExpect(jsonPath("$.genres[0].id").value(18))
                .andExpect(jsonPath("$.genres[0].name").value("Drama"));

        verify(tmdbService).getTvGenres("fr-FR");
    }

    @Test
    @DisplayName("GET /api/tv/{id}/watch-providers returns 200")
    void getTvWatchProviders_returnsOk() throws Exception {
        when(tmdbService.getTvWatchProviders(1396L, "fr-FR")).thenReturn(sampleWatchProviders);

        mockMvc.perform(get("/api/tv/1396/watch-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("https://www.themoviedb.org/tv/1396/watch"))
                .andExpect(jsonPath("$.flatrate[0].provider_name").value("Netflix"));

        verify(tmdbService).getTvWatchProviders(1396L, "fr-FR");
    }

    @Test
    @DisplayName("Default lang parameter is fr-FR when not specified")
    void defaultLang_isFrFR() throws Exception {
        when(tmdbService.getTvGenres("fr-FR")).thenReturn(sampleGenreList);

        mockMvc.perform(get("/api/tv/genres"))
                .andExpect(status().isOk());

        verify(tmdbService).getTvGenres("fr-FR");
    }

    @Test
    @DisplayName("GET /api/tv/trending with page=0 returns 400 (constraint violation)")
    void getTrendingTv_invalidPage_returnsError() throws Exception {
        mockMvc.perform(get("/api/tv/trending")
                        .param("page", "0"))
                .andExpect(status().isBadRequest());
    }
}
