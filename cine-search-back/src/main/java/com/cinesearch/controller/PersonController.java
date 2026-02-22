package com.cinesearch.controller;

import com.cinesearch.dto.PersonCreditsResponse;
import com.cinesearch.dto.PersonSearchResponse;
import com.cinesearch.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller exposing TMDB person search and filmography endpoints. */
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final TmdbService tmdbService;

    public PersonController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/popular")
    public ResponseEntity<PersonSearchResponse> getPopularPersons(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getPopularPersons(page));
    }

    @GetMapping("/search")
    public ResponseEntity<PersonSearchResponse> searchPersons(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.searchPersons(query, page));
    }

    @GetMapping("/{id}/movies")
    public ResponseEntity<PersonCreditsResponse> getPersonMovies(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbService.getPersonMovies(id));
    }
}
