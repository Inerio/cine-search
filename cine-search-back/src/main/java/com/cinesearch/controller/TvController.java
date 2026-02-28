package com.cinesearch.controller;

import com.cinesearch.dto.GenreListResponse;
import com.cinesearch.dto.MovieListResponse;
import com.cinesearch.dto.TvDetailDto;
import com.cinesearch.dto.WatchProvidersResponse;
import com.cinesearch.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tv")
public class TvController {

    private final TmdbService tmdbService;

    public TvController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/trending")
    public ResponseEntity<MovieListResponse> getTrendingTv(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTrendingTv(page, lang));
    }

    @GetMapping("/search")
    public ResponseEntity<MovieListResponse> searchTv(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.searchTv(query, page, lang));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TvDetailDto> getTvDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTvDetail(id, lang));
    }

    @GetMapping("/discover")
    public ResponseEntity<MovieListResponse> discoverTv(
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer runtimeGte,
            @RequestParam(required = false) Integer runtimeLte,
            @RequestParam(required = false) String decadeStart,
            @RequestParam(required = false) String decadeEnd,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.discoverTvAdvanced(
                genreId, minRating, language, sortBy,
                runtimeGte, runtimeLte, decadeStart, decadeEnd, page, lang));
    }

    @GetMapping("/genres")
    public ResponseEntity<GenreListResponse> getTvGenres(
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTvGenres(lang));
    }

    @GetMapping("/{id}/watch-providers")
    public ResponseEntity<WatchProvidersResponse> getTvWatchProviders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        return ResponseEntity.ok(tmdbService.getTvWatchProviders(id, lang));
    }
}
