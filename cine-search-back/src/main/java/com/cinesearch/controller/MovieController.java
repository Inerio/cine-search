package com.cinesearch.controller;

import com.cinesearch.dto.GenreListResponse;
import com.cinesearch.dto.MovieDetailDto;
import com.cinesearch.dto.MovieListResponse;
import com.cinesearch.dto.WatchProvidersResponse;
import com.cinesearch.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller exposing TMDB movie endpoints (trending, popular, search, detail, discover, genres). */
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbService tmdbService;

    public MovieController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/trending")
    public ResponseEntity<MovieListResponse> getTrending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTrending(page, lang));
    }

    @GetMapping("/popular")
    public ResponseEntity<MovieListResponse> getPopular(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getPopular(page, lang));
    }

    @GetMapping("/search")
    public ResponseEntity<MovieListResponse> searchMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.searchMovies(query, page, lang));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailDto> getMovieDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getMovieDetail(id, lang));
    }

    @GetMapping("/discover")
    public ResponseEntity<MovieListResponse> discoverMovies(
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer runtimeGte,
            @RequestParam(required = false) Integer runtimeLte,
            @RequestParam(required = false) Long directorId,
            @RequestParam(required = false) String decadeStart,
            @RequestParam(required = false) String decadeEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.discoverMoviesAdvanced(
                genreId, year, minRating, language, sortBy,
                runtimeGte, runtimeLte, directorId,
                decadeStart, decadeEnd, page, lang));
    }

    @GetMapping("/genres")
    public ResponseEntity<GenreListResponse> getGenres(
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getGenres(lang));
    }

    @GetMapping("/{id}/watch-providers")
    public ResponseEntity<WatchProvidersResponse> getWatchProviders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getWatchProviders(id, lang));
    }
}
