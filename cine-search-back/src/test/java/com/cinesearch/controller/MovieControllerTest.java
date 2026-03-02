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

@WebMvcTest(MovieController.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbService tmdbService;

    private MovieListResponse sampleMovieList;
    private MovieDetailDto sampleMovieDetail;
    private GenreListResponse sampleGenreList;
    private WatchProvidersResponse sampleWatchProviders;

    @BeforeEach
    void setUp() {
        MovieDto movie = new MovieDto(
                550L, "Fight Club", "An insomniac office worker...",
                "/pB8BM7pdSp6B6Ih7QI4S2t0POoJ.jpg", "/fCayJrkfRaCRCTh8GqN30f8oyQF.jpg",
                "1999-10-15", 8.4, 26000, 63.0,
                List.of(18, 53), "en", null, "movie"
        );
        sampleMovieList = new MovieListResponse(1, List.of(movie), 10, 200);

        sampleMovieDetail = new MovieDetailDto(
                550L, "Fight Club", "An insomniac office worker...",
                "/pB8BM7pdSp6B6Ih7QI4S2t0POoJ.jpg", "/fCayJrkfRaCRCTh8GqN30f8oyQF.jpg",
                "1999-10-15", 8.4, 26000, 139, "Mischief. Mayhem. Soap.",
                63000000L, 100853753L, "Released",
                List.of(new MovieDetailDto.GenreDto(18, "Drama")),
                new MovieDetailDto.CreditsDto(
                        List.of(new MovieDetailDto.CastMemberDto(819L, "Edward Norton", "The Narrator", "/5XBzD5WuTyVQZeS4VI25z2moSimz.jpg", 0)),
                        List.of(new MovieDetailDto.CrewMemberDto(7467L, "David Fincher", "Director", "Directing", "/tpEczFclQZeKAiCeKZZ0adRvtfz.jpg"))
                )
        );

        sampleGenreList = new GenreListResponse(
                List.of(new GenreListResponse.Genre(28, "Action"), new GenreListResponse.Genre(18, "Drama"))
        );

        sampleWatchProviders = new WatchProvidersResponse(
                "https://www.themoviedb.org/movie/550/watch",
                List.of(new WatchProvidersResponse.ProviderDto(8, "Netflix", "/t2yyOv40HZeVlLjYsCsPHnWLk4W.jpg")),
                List.of(),
                List.of()
        );
    }

    @Test
    @DisplayName("GET /api/movies/trending returns 200 with movie list")
    void getTrending_returnsOk() throws Exception {
        when(tmdbService.getTrending(1, "fr-FR")).thenReturn(sampleMovieList);

        mockMvc.perform(get("/api/movies/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.results[0].id").value(550))
                .andExpect(jsonPath("$.results[0].title").value("Fight Club"))
                .andExpect(jsonPath("$.total_pages").value(10))
                .andExpect(jsonPath("$.total_results").value(200));

        verify(tmdbService).getTrending(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/trending with custom page and lang")
    void getTrending_withCustomParams() throws Exception {
        when(tmdbService.getTrending(3, "en-US")).thenReturn(sampleMovieList);

        mockMvc.perform(get("/api/movies/trending")
                        .param("page", "3")
                        .param("lang", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));

        verify(tmdbService).getTrending(3, "en-US");
    }

    @Test
    @DisplayName("GET /api/movies/popular returns 200 with movie list")
    void getPopular_returnsOk() throws Exception {
        when(tmdbService.getPopular(1, "fr-FR")).thenReturn(sampleMovieList);

        mockMvc.perform(get("/api/movies/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].id").value(550));

        verify(tmdbService).getPopular(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/search returns 200 with search results")
    void searchMovies_returnsOk() throws Exception {
        when(tmdbService.searchMovies("Fight Club", 1, "fr-FR")).thenReturn(sampleMovieList);

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Fight Club"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].title").value("Fight Club"));

        verify(tmdbService).searchMovies("Fight Club", 1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/search without query returns 500 (missing required param)")
    void searchMovies_missingQuery_returnsError() throws Exception {
        mockMvc.perform(get("/api/movies/search"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/movies/{id} returns 200 with movie detail")
    void getMovieDetail_returnsOk() throws Exception {
        when(tmdbService.getMovieDetail(550L, "fr-FR")).thenReturn(sampleMovieDetail);

        mockMvc.perform(get("/api/movies/550"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(550))
                .andExpect(jsonPath("$.title").value("Fight Club"))
                .andExpect(jsonPath("$.runtime").value(139))
                .andExpect(jsonPath("$.genres[0].name").value("Drama"));

        verify(tmdbService).getMovieDetail(550L, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/discover returns 200 with discovered movies")
    void discoverMovies_returnsOk() throws Exception {
        when(tmdbService.discoverMoviesAdvanced(
                eq(28), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq("fr-FR")
        )).thenReturn(sampleMovieList);

        mockMvc.perform(get("/api/movies/discover")
                        .param("genreId", "28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());

        verify(tmdbService).discoverMoviesAdvanced(
                eq(28), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq("fr-FR")
        );
    }

    @Test
    @DisplayName("GET /api/movies/genres returns 200 with genre list")
    void getGenres_returnsOk() throws Exception {
        when(tmdbService.getGenres("fr-FR")).thenReturn(sampleGenreList);

        mockMvc.perform(get("/api/movies/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").isArray())
                .andExpect(jsonPath("$.genres[0].id").value(28))
                .andExpect(jsonPath("$.genres[0].name").value("Action"));

        verify(tmdbService).getGenres("fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/{id}/watch-providers returns 200")
    void getWatchProviders_returnsOk() throws Exception {
        when(tmdbService.getWatchProviders(550L, "fr-FR")).thenReturn(sampleWatchProviders);

        mockMvc.perform(get("/api/movies/550/watch-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("https://www.themoviedb.org/movie/550/watch"))
                .andExpect(jsonPath("$.flatrate[0].provider_name").value("Netflix"));

        verify(tmdbService).getWatchProviders(550L, "fr-FR");
    }

    @Test
    @DisplayName("Default lang parameter is fr-FR when not specified")
    void defaultLang_isFrFR() throws Exception {
        when(tmdbService.getGenres("fr-FR")).thenReturn(sampleGenreList);

        mockMvc.perform(get("/api/movies/genres"))
                .andExpect(status().isOk());

        verify(tmdbService).getGenres("fr-FR");
    }

    @Test
    @DisplayName("GET /api/movies/trending with page=0 returns 500 (constraint violation)")
    void getTrending_invalidPage_returnsError() throws Exception {
        mockMvc.perform(get("/api/movies/trending")
                        .param("page", "0"))
                .andExpect(status().isBadRequest());
    }
}
