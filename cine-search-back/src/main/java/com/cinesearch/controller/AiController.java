package com.cinesearch.controller;

import com.cinesearch.dto.*;
import com.cinesearch.service.GroqService;
import com.cinesearch.service.TmdbService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * AI-powered search endpoint. Accepts natural language queries,
 * extracts structured filters via Groq LLM, and resolves results
 * through a multi-step TMDB fallback cascade.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GroqService groqService;
    private final TmdbService tmdbService;

    public AiController(GroqService groqService, TmdbService tmdbService) {
        this.groqService = groqService;
        this.tmdbService = tmdbService;
    }

    @PostMapping("/parse")
    public ResponseEntity<AiSearchResponse> parse(
            @Valid @RequestBody AiParseRequest request,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        String userText = request.getText().trim();
        log.info("AI parse request: '{}' (lang={})", userText, lang);

        // 1. LLM structured extraction
        AiMovieQuery parsed = groqService.parseUserQuery(userText);

        // 2. Unknown intent → empty
        if ("unknown".equals(parsed.getIntent())) {
            log.info("Intent=unknown, returning empty results");
            return ResponseEntity.ok(new AiSearchResponse(parsed, List.of(), 0));
        }

        // 3. Resolve results with fallback cascade
        SearchResult result = resolveResults(parsed, userText, lang);

        log.info("AI search returned {} results (intent={})", result.movies.size(), parsed.getIntent());
        return ResponseEntity.ok(new AiSearchResponse(parsed, result.movies, result.total));
    }

    // ============================
    // Fallback cascade strategy
    // ============================

    private record SearchResult(List<MovieDto> movies, int total) {}

    /**
     * Multi-step TMDB resolution. Tries the most specific approach first,
     * then falls back to broader searches until we get results.
     */
    private SearchResult resolveResults(AiMovieQuery parsed, String userText, String lang) {

        // Step 1: If LLM identified a title → search by title (most precise)
        if (parsed.getTitle() != null && !parsed.getTitle().isBlank()) {
            log.info("Step 1: searching by title '{}'", parsed.getTitle());
            SearchResult r = searchTmdb(parsed.getTitle(), lang);
            if (!r.movies.isEmpty()) return r;
        }

        // Step 2: If LLM extracted a query → search by query
        if (parsed.getQuery() != null && !parsed.getQuery().isBlank()) {
            log.info("Step 2: searching by query '{}'", parsed.getQuery());
            SearchResult r = searchTmdb(parsed.getQuery(), lang);
            if (!r.movies.isEmpty()) return r;

            // Step 2.5: query returned 0 → try individual keywords (longest first)
            log.info("Step 2.5: trying individual keywords from query");
            SearchResult kwResult = searchByKeywords(parsed.getQuery(), lang);
            if (!kwResult.movies.isEmpty()) return kwResult;
        }

        // Step 3: If we have filters (genres/year/language/sort) → discover
        if (hasFilters(parsed)) {
            log.info("Step 3: discover with filters genres={}, year={}, lang={}",
                    parsed.getGenres(), parsed.getYear(), parsed.getLanguage());
            SearchResult r = discoverTmdb(parsed, lang);
            if (!r.movies.isEmpty()) return r;
        }

        // Step 4: Last resort — search TMDB with the raw user text
        log.info("Step 4: fallback search with raw userText");
        SearchResult r = searchTmdb(userText, lang);
        if (!r.movies.isEmpty()) return r;

        // Step 5: Absolute last resort — discover popular movies in detected genres
        if (parsed.getGenres() != null && !parsed.getGenres().isEmpty()) {
            log.info("Step 5: fallback discover popular in genre");
            return discoverTmdb(parsed, lang);
        }

        log.info("No results found after all fallback steps");
        return new SearchResult(List.of(), 0);
    }

    /**
     * Splits a query into individual keywords (>3 chars), sorted longest first,
     * and searches TMDB with each until we get results.
     */
    private SearchResult searchByKeywords(String query, String lang) {
        List<String> keywords = Arrays.stream(query.split("\\s+"))
                .filter(w -> w.length() > 3)
                .sorted((a, b) -> b.length() - a.length())
                .limit(4)
                .toList();

        for (String keyword : keywords) {
            log.info("  keyword search: '{}'", keyword);
            SearchResult r = searchTmdb(keyword, lang);
            if (!r.movies.isEmpty()) return r;
        }
        return new SearchResult(List.of(), 0);
    }

    private SearchResult searchTmdb(String query, String lang) {
        MovieListResponse response = tmdbService.searchMovies(query, 1, lang);
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            return new SearchResult(response.getResults(),
                    response.getTotalResults() != null ? response.getTotalResults() : response.getResults().size());
        }
        return new SearchResult(List.of(), 0);
    }

    private SearchResult discoverTmdb(AiMovieQuery parsed, String lang) {
        MovieListResponse response = tmdbService.discoverMoviesAdvanced(
                resolveGenreId(parsed.getGenres()),
                parsed.getYear(),
                resolveSortRating(parsed.getSort()),
                resolveLanguageCode(parsed.getLanguage()),
                resolveTmdbSort(parsed.getSort()),
                1,
                lang
        );
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            // Cap totalResults to actual page size — discover can return 200K+ total
            return new SearchResult(response.getResults(), response.getResults().size());
        }
        return new SearchResult(List.of(), 0);
    }

    // ============================
    // Helpers
    // ============================

    private boolean hasFilters(AiMovieQuery q) {
        return (q.getGenres() != null && !q.getGenres().isEmpty())
                || q.getYear() != null
                || q.getLanguage() != null
                || (q.getSort() != null && !"relevance".equals(q.getSort()));
    }

    // --- Genre mapping ---

    private static final Map<String, Integer> GENRE_MAP = Map.ofEntries(
            Map.entry("action", 28),
            Map.entry("adventure", 12), Map.entry("aventure", 12),
            Map.entry("animation", 16),
            Map.entry("comedy", 35), Map.entry("comédie", 35), Map.entry("comedie", 35),
            Map.entry("crime", 80),
            Map.entry("documentary", 99), Map.entry("documentaire", 99),
            Map.entry("drama", 18), Map.entry("drame", 18),
            Map.entry("family", 10751), Map.entry("famille", 10751),
            Map.entry("fantasy", 14), Map.entry("fantaisie", 14), Map.entry("fantastique", 14),
            Map.entry("history", 36), Map.entry("histoire", 36), Map.entry("historique", 36),
            Map.entry("horror", 27), Map.entry("horreur", 27),
            Map.entry("music", 10402), Map.entry("musique", 10402),
            Map.entry("mystery", 9648), Map.entry("mystère", 9648), Map.entry("mystere", 9648),
            Map.entry("romance", 10749),
            Map.entry("sci-fi", 878), Map.entry("science-fiction", 878), Map.entry("science fiction", 878), Map.entry("sf", 878),
            Map.entry("thriller", 53),
            Map.entry("war", 10752), Map.entry("guerre", 10752),
            Map.entry("western", 37)
    );

    private Integer resolveGenreId(List<String> genres) {
        if (genres == null || genres.isEmpty()) return null;
        return GENRE_MAP.get(genres.getFirst().toLowerCase());
    }

    // --- Language ISO 639-1 mapping ---

    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
            Map.entry("french", "fr"), Map.entry("français", "fr"), Map.entry("francais", "fr"),
            Map.entry("english", "en"), Map.entry("anglais", "en"),
            Map.entry("spanish", "es"), Map.entry("espagnol", "es"),
            Map.entry("german", "de"), Map.entry("allemand", "de"),
            Map.entry("italian", "it"), Map.entry("italien", "it"),
            Map.entry("japanese", "ja"), Map.entry("japonais", "ja"),
            Map.entry("korean", "ko"), Map.entry("coréen", "ko"), Map.entry("coreen", "ko"),
            Map.entry("portuguese", "pt"), Map.entry("portugais", "pt"),
            Map.entry("chinese", "zh"), Map.entry("chinois", "zh"),
            Map.entry("hindi", "hi"), Map.entry("arabic", "ar"), Map.entry("arabe", "ar"),
            Map.entry("russian", "ru"), Map.entry("russe", "ru"),
            Map.entry("swedish", "sv"), Map.entry("suédois", "sv"),
            Map.entry("danish", "da"), Map.entry("danois", "da"),
            Map.entry("thai", "th"), Map.entry("turkish", "tr"), Map.entry("turc", "tr")
    );

    private String resolveLanguageCode(String language) {
        if (language == null) return null;
        return LANGUAGE_MAP.get(language.toLowerCase());
    }

    // --- Sort mapping ---

    private Double resolveSortRating(String sort) {
        if ("rating".equals(sort)) return 7.0;
        return null;
    }

    private String resolveTmdbSort(String sort) {
        if (sort == null) return null;
        return switch (sort) {
            case "rating" -> "vote_average.desc";
            case "popularity" -> "popularity.desc";
            case "recent" -> "primary_release_date.desc";
            default -> null;
        };
    }
}
