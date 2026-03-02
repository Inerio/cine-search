package com.cinesearch.controller;

import com.cinesearch.dto.MovieDto;
import com.cinesearch.dto.PersonCreditsResponse;
import com.cinesearch.dto.PersonDto;
import com.cinesearch.dto.PersonSearchResponse;
import com.cinesearch.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbService tmdbService;

    private PersonSearchResponse samplePersonSearch;
    private PersonDto samplePersonDetail;
    private PersonCreditsResponse samplePersonCredits;

    @BeforeEach
    void setUp() {
        PersonDto person = new PersonDto(
                287L, "Brad Pitt", "/cckcYc2v0yh1tc9QjRelptcOBko.jpg",
                "Acting", 85.5, 2, List.of()
        );
        samplePersonSearch = new PersonSearchResponse(1, List.of(person), 3, 50);

        samplePersonDetail = new PersonDto(
                287L, "Brad Pitt", "/cckcYc2v0yh1tc9QjRelptcOBko.jpg",
                "Acting", 85.5, 2, List.of()
        );

        MovieDto movie = new MovieDto(
                550L, "Fight Club", "An insomniac office worker...",
                "/pB8BM7pdSp6B6Ih7QI4S2t0POoJ.jpg", "/fCayJrkfRaCRCTh8GqN30f8oyQF.jpg",
                "1999-10-15", 8.4, 26000, 63.0,
                List.of(18, 53), "en", null, "movie"
        );
        samplePersonCredits = new PersonCreditsResponse(287L, List.of(movie), List.of());
    }

    @Test
    @DisplayName("GET /api/persons/popular returns 200 with person list")
    void getPopularPersons_returnsOk() throws Exception {
        when(tmdbService.getPopularPersons(1, "fr-FR")).thenReturn(samplePersonSearch);

        mockMvc.perform(get("/api/persons/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.results[0].id").value(287))
                .andExpect(jsonPath("$.results[0].name").value("Brad Pitt"))
                .andExpect(jsonPath("$.total_pages").value(3))
                .andExpect(jsonPath("$.total_results").value(50));

        verify(tmdbService).getPopularPersons(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/popular with custom page and lang")
    void getPopularPersons_withCustomParams() throws Exception {
        when(tmdbService.getPopularPersons(2, "en-US")).thenReturn(samplePersonSearch);

        mockMvc.perform(get("/api/persons/popular")
                        .param("page", "2")
                        .param("lang", "en-US"))
                .andExpect(status().isOk());

        verify(tmdbService).getPopularPersons(2, "en-US");
    }

    @Test
    @DisplayName("GET /api/persons/trending returns 200 with trending persons")
    void getTrendingPersons_returnsOk() throws Exception {
        when(tmdbService.getTrendingPersons(1, "fr-FR")).thenReturn(samplePersonSearch);

        mockMvc.perform(get("/api/persons/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name").value("Brad Pitt"));

        verify(tmdbService).getTrendingPersons(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/search returns 200 with search results")
    void searchPersons_returnsOk() throws Exception {
        when(tmdbService.searchPersons("Brad Pitt", 1, "fr-FR")).thenReturn(samplePersonSearch);

        mockMvc.perform(get("/api/persons/search")
                        .param("query", "Brad Pitt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name").value("Brad Pitt"));

        verify(tmdbService).searchPersons("Brad Pitt", 1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/search without query returns 500 (missing required param)")
    void searchPersons_missingQuery_returnsError() throws Exception {
        mockMvc.perform(get("/api/persons/search"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/persons/{id} returns 200 with person detail")
    void getPersonDetails_returnsOk() throws Exception {
        when(tmdbService.getPersonDetails(287L, "fr-FR")).thenReturn(samplePersonDetail);

        mockMvc.perform(get("/api/persons/287"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(287))
                .andExpect(jsonPath("$.name").value("Brad Pitt"))
                .andExpect(jsonPath("$.known_for_department").value("Acting"));

        verify(tmdbService).getPersonDetails(287L, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/{id}/movies returns 200 with movie credits")
    void getPersonMovies_returnsOk() throws Exception {
        when(tmdbService.getPersonMovies(287L, "fr-FR")).thenReturn(samplePersonCredits);

        mockMvc.perform(get("/api/persons/287/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(287))
                .andExpect(jsonPath("$.cast[0].id").value(550))
                .andExpect(jsonPath("$.cast[0].title").value("Fight Club"))
                .andExpect(jsonPath("$.crew").isArray());

        verify(tmdbService).getPersonMovies(287L, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/{id}/tv-shows returns 200 with TV credits")
    void getPersonTvShows_returnsOk() throws Exception {
        when(tmdbService.getPersonTvShows(287L, "fr-FR")).thenReturn(samplePersonCredits);

        mockMvc.perform(get("/api/persons/287/tv-shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(287));

        verify(tmdbService).getPersonTvShows(287L, "fr-FR");
    }

    @Test
    @DisplayName("Default lang parameter is fr-FR when not specified")
    void defaultLang_isFrFR() throws Exception {
        when(tmdbService.getPopularPersons(1, "fr-FR")).thenReturn(samplePersonSearch);

        mockMvc.perform(get("/api/persons/popular"))
                .andExpect(status().isOk());

        verify(tmdbService).getPopularPersons(1, "fr-FR");
    }

    @Test
    @DisplayName("GET /api/persons/popular with page=0 returns 400 (constraint violation)")
    void getPopularPersons_invalidPage_returnsError() throws Exception {
        mockMvc.perform(get("/api/persons/popular")
                        .param("page", "0"))
                .andExpect(status().isBadRequest());
    }
}
