package com.cinesearch.controller;

import com.cinesearch.dto.*;
import com.cinesearch.service.GroqService;
import com.cinesearch.service.TmdbService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered search endpoint. Accepts natural language queries,
 * extracts structured filters via Groq LLM, and resolves results
 * through a multi-step TMDB fallback cascade with best-match scoring
 * and similar movie recommendations.
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
        String userText = request.text().trim();
        log.info("AI parse request: '{}' (lang={})", userText, lang);

        // 1. LLM structured extraction
        AiMovieQuery parsed = groqService.parseUserQuery(userText);

        // 2. Unknown intent → empty
        if ("unknown".equals(parsed.getIntent())) {
            log.info("Intent=unknown, returning empty results");
            return ResponseEntity.ok(new AiSearchResponse(parsed, null, List.of(), List.of(), 0));
        }

        // 3. Resolve results with enhanced fallback cascade
        EnhancedResult result = resolveResults(parsed, userText, lang);

        log.info("AI search: bestMatch={}, similar={}, results={} (intent={})",
                result.bestMatch != null ? result.bestMatch.title() : "none",
                result.similarMovies.size(),
                result.results.size(),
                parsed.getIntent());

        return ResponseEntity.ok(new AiSearchResponse(
                parsed, result.bestMatch, result.similarMovies, result.results, result.total));
    }

    // ============================
    // Enhanced resolution cascade
    // ============================

    private record EnhancedResult(MovieDto bestMatch, List<MovieDto> similarMovies,
                                   List<MovieDto> results, int total) {}

    /**
     * Two-phase resolution:
     * Phase A: Find the exact movie (best match)
     * Phase B: Get similar movies if best match found
     */
    private EnhancedResult resolveResults(AiMovieQuery parsed, String userText, String lang) {
        MovieDto bestMatch = null;
        List<MovieDto> results = List.of();

        // === PHASE A: Find the exact movie ===

        // Step 1: Title search (most precise)
        if (parsed.getTitle() != null && !parsed.getTitle().isBlank()) {
            log.info("Step 1: searching by title '{}'", parsed.getTitle());
            SearchResult r = searchTmdb(parsed.getTitle(), lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed);
                results = r.movies;
            } else if (parsed.getYear() != null) {
                // Step 1b: title + year for disambiguation
                log.info("Step 1b: searching by title + year '{} {}'", parsed.getTitle(), parsed.getYear());
                r = searchTmdb(parsed.getTitle() + " " + parsed.getYear(), lang);
                if (!r.movies.isEmpty()) {
                    bestMatch = pickBestMatch(r.movies, parsed);
                    results = r.movies;
                }
            }
        }

        // Step 2: Alternate titles (NEW)
        if (bestMatch == null && parsed.getAlternateTitles() != null) {
            for (String altTitle : parsed.getAlternateTitles()) {
                log.info("Step 2: trying alternate title '{}'", altTitle);
                SearchResult r = searchTmdb(altTitle, lang);
                if (!r.movies.isEmpty()) {
                    bestMatch = pickBestMatch(r.movies, parsed);
                    results = r.movies;
                    break;
                }
            }
        }

        // Step 3: Actor-based search (NEW)
        if (bestMatch == null && parsed.getActors() != null && !parsed.getActors().isEmpty()) {
            log.info("Step 3: searching by actor '{}'", parsed.getActors().getFirst());
            bestMatch = searchByActor(parsed, lang);
            if (bestMatch != null && results.isEmpty()) {
                // Use title search with the found movie's title for full results
                SearchResult r = searchTmdb(bestMatch.title(), lang);
                results = r.movies.isEmpty() ? List.of(bestMatch) : r.movies;
            }
        }

        // Step 4: Query-based search
        if (bestMatch == null && parsed.getQuery() != null && !parsed.getQuery().isBlank()) {
            log.info("Step 4: searching by query '{}'", parsed.getQuery());
            SearchResult r = searchTmdb(parsed.getQuery(), lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed);
                results = r.movies;
            }
        }

        // Step 5: Keyword-based search (enhanced)
        if (bestMatch == null && parsed.getKeywords() != null && !parsed.getKeywords().isEmpty()) {
            log.info("Step 5: searching by keywords {}", parsed.getKeywords());
            // Try combined keywords first
            String combined = String.join(" ", parsed.getKeywords());
            SearchResult r = searchTmdb(combined, lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed);
                results = r.movies;
            } else {
                // Try individual keywords (longest first)
                SearchResult kwResult = searchByKeywords(combined, lang);
                if (!kwResult.movies.isEmpty()) {
                    bestMatch = pickBestMatch(kwResult.movies, parsed);
                    results = kwResult.movies;
                }
            }
        }

        // Step 6: Discover with filters (multi-genre)
        if (results.isEmpty() && hasFilters(parsed)) {
            log.info("Step 6: discover with filters genres={}, year={}, lang={}",
                    parsed.getGenres(), parsed.getYear(), parsed.getLanguage());
            SearchResult r = discoverTmdb(parsed, lang);
            if (!r.movies.isEmpty()) {
                results = r.movies;
            }
        }

        // Step 7: Raw user text fallback
        if (results.isEmpty()) {
            log.info("Step 7: fallback search with raw userText");
            SearchResult r = searchTmdb(userText, lang);
            results = r.movies;
        }

        // Step 8: Popular discover in genre fallback
        if (results.isEmpty() && parsed.getGenres() != null && !parsed.getGenres().isEmpty()) {
            log.info("Step 8: fallback discover popular in genre");
            SearchResult r = discoverTmdb(parsed, lang);
            results = r.movies;
        }

        // === PHASE B: Get similar movies ===
        List<MovieDto> similarMovies = List.of();
        if (bestMatch != null && bestMatch.id() != null) {
            log.info("Phase B: fetching similar movies for '{}'", bestMatch.title());
            try {
                MovieListResponse similar = tmdbService.getSimilarMovies(bestMatch.id(), 1, lang);
                if (similar != null && similar.results() != null) {
                    similarMovies = similar.results().stream().limit(12).toList();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch similar movies: {}", e.getMessage());
            }
        }

        // Remove bestMatch from results to avoid duplicate display
        if (bestMatch != null) {
            final Long bestId = bestMatch.id();
            results = results.stream().filter(m -> !Objects.equals(m.id(), bestId)).toList();
        }

        int total = results.size() + (bestMatch != null ? 1 : 0);
        return new EnhancedResult(bestMatch, similarMovies, results, total);
    }

    // ============================
    // Best match scoring
    // ============================

    /**
     * Scores search results against LLM metadata (year, genres)
     * to find the single best matching movie.
     */
    private MovieDto pickBestMatch(List<MovieDto> results, AiMovieQuery parsed) {
        if (results.isEmpty()) return null;
        if (results.size() == 1) return results.getFirst();

        MovieDto best = results.getFirst();
        int bestScore = 0;

        for (MovieDto movie : results) {
            int score = 0;
            // Year match (strong signal)
            if (parsed.getYear() != null && movie.release_date() != null) {
                try {
                    int movieYear = Integer.parseInt(movie.release_date().substring(0, 4));
                    if (movieYear == parsed.getYear()) score += 10;
                    else if (Math.abs(movieYear - parsed.getYear()) <= 1) score += 5;
                } catch (Exception ignored) {}
            }
            // Genre overlap
            if (parsed.getGenres() != null && movie.genre_ids() != null) {
                for (String genre : parsed.getGenres()) {
                    Integer genreId = GENRE_MAP.get(genre.toLowerCase());
                    if (genreId != null && movie.genre_ids().contains(genreId)) {
                        score += 3;
                    }
                }
            }
            // Popularity tie-breaker
            if (movie.popularity() != null) {
                score += (int) Math.min(movie.popularity() / 10, 5);
            }
            if (score > bestScore) {
                bestScore = score;
                best = movie;
            }
        }
        return best;
    }

    // ============================
    // Actor-based search (NEW)
    // ============================

    /**
     * Searches for an actor on TMDB, retrieves their filmography,
     * and filters/scores it against the parsed query metadata.
     */
    private MovieDto searchByActor(AiMovieQuery parsed, String lang) {
        try {
            String actorName = parsed.getActors().getFirst();
            PersonSearchResponse personSearch = tmdbService.searchPersons(actorName, 1, lang);
            if (personSearch == null || personSearch.results() == null || personSearch.results().isEmpty()) {
                return null;
            }

            Long personId = personSearch.results().getFirst().id();
            PersonCreditsResponse credits = tmdbService.getPersonMovies(personId, lang);
            if (credits == null || credits.cast() == null || credits.cast().isEmpty()) {
                return null;
            }

            // Filter and score actor's filmography
            List<MovieDto> movies = credits.cast().stream()
                    .filter(m -> m.title() != null)
                    // Filter by year if specified
                    .filter(m -> {
                        if (parsed.getYear() == null) return true;
                        if (m.release_date() == null || m.release_date().length() < 4) return false;
                        try {
                            int year = Integer.parseInt(m.release_date().substring(0, 4));
                            return Math.abs(year - parsed.getYear()) <= 2;
                        } catch (Exception e) { return false; }
                    })
                    // Filter by genre if specified
                    .filter(m -> {
                        if (parsed.getGenres() == null || parsed.getGenres().isEmpty()) return true;
                        if (m.genre_ids() == null) return false;
                        return parsed.getGenres().stream().anyMatch(g -> {
                            Integer gid = GENRE_MAP.get(g.toLowerCase());
                            return gid != null && m.genre_ids().contains(gid);
                        });
                    })
                    .sorted(Comparator.comparingDouble((MovieDto m) ->
                            m.popularity() != null ? m.popularity() : 0).reversed())
                    .limit(20)
                    .toList();

            if (!movies.isEmpty()) {
                log.info("  Actor search found {} movies, best: '{}'", movies.size(), movies.getFirst().title());
                return movies.getFirst();
            }
        } catch (Exception e) {
            log.warn("Actor search failed: {}", e.getMessage());
        }
        return null;
    }

    // ============================
    // Existing helpers (enhanced)
    // ============================

    private record SearchResult(List<MovieDto> movies, int total) {}

    private SearchResult searchTmdb(String query, String lang) {
        MovieListResponse response = tmdbService.searchMovies(query, 1, lang);
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            return new SearchResult(response.results(),
                    response.total_results() != null ? response.total_results() : response.results().size());
        }
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

    /**
     * Discover using ALL genres (multi-genre) instead of just the first.
     */
    private SearchResult discoverTmdb(AiMovieQuery parsed, String lang) {
        String genreIds = resolveGenreIds(parsed.getGenres());
        MovieListResponse response = tmdbService.discoverMoviesWithGenreIds(
                genreIds,
                parsed.getYear(),
                resolveSortRating(parsed.getSort()),
                resolveLanguageCode(parsed.getLanguage()),
                resolveTmdbSort(parsed.getSort()),
                1,
                lang
        );
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            return new SearchResult(response.results(), response.results().size());
        }
        return new SearchResult(List.of(), 0);
    }

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

    /**
     * Returns ALL genre IDs as a comma-separated string (e.g. "28,878,53")
     * instead of just the first genre.
     */
    private String resolveGenreIds(List<String> genres) {
        if (genres == null || genres.isEmpty()) return null;
        String ids = genres.stream()
                .map(g -> GENRE_MAP.get(g.toLowerCase()))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return ids.isEmpty() ? null : ids;
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
