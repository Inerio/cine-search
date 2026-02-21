package com.cinesearch.controller;

import com.cinesearch.dto.GenreListResponse;
import com.cinesearch.dto.MovieDetailDto;
import com.cinesearch.dto.MovieListResponse;
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
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getTrending(page));
    }

    @GetMapping("/popular")
    public ResponseEntity<MovieListResponse> getPopular(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getPopular(page));
    }

    @GetMapping("/search")
    public ResponseEntity<MovieListResponse> searchMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.searchMovies(query, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailDto> getMovieDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbService.getMovieDetail(id));
    }

    @GetMapping("/discover")
    public ResponseEntity<MovieListResponse> discoverMovies(
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.discoverMovies(genreId, year, minRating, page));
    }

    @GetMapping("/genres")
    public ResponseEntity<GenreListResponse> getGenres() {
        return ResponseEntity.ok(tmdbService.getGenres());
    }
}
