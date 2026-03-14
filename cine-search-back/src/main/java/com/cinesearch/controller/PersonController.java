package com.cinesearch.controller;

import com.cinesearch.dto.PersonCreditsResponse;
import com.cinesearch.dto.PersonDto;
import com.cinesearch.dto.PersonSearchResponse;
import com.cinesearch.service.TmdbService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** REST controller exposing TMDB person search and filmography endpoints. */
@Validated
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final TmdbService tmdbService;

    public PersonController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/popular")
    public ResponseEntity<PersonSearchResponse> getPopularPersons(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getPopularPersons(page, lang));
    }

    @GetMapping("/trending")
    public ResponseEntity<PersonSearchResponse> getTrendingPersons(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTrendingPersons(page, lang));
    }

    @GetMapping("/search")
    public ResponseEntity<PersonSearchResponse> searchPersons(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.searchPersons(query, page, lang));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> getPersonDetails(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getPersonDetails(id, lang));
    }

    @GetMapping("/{id}/movies")
    public ResponseEntity<PersonCreditsResponse> getPersonMovies(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getPersonMovies(id, lang));
    }

    @GetMapping("/{id}/tv-shows")
    public ResponseEntity<PersonCreditsResponse> getPersonTvShows(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getPersonTvShows(id, lang));
    }
}
