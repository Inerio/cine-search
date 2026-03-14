package com.cinesearch.controller;

import com.cinesearch.dto.*;
import com.cinesearch.service.GroqService;
import com.cinesearch.service.MovieScoringService;
import com.cinesearch.service.TmdbService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AI-powered search endpoint. Accepts natural language queries,
 * extracts structured filters via Groq LLM, and resolves results
 * through a multi-step TMDB fallback cascade with best-match scoring
 * and similar movie recommendations.
 * Supports movie/tv/all media types.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    // Search cascade limits
    private static final int MAX_KEYWORD_PAIRS = 20;
    private static final int MAX_KEYWORD_TRIPLETS = 10;
    private static final int SIMILAR_MOVIES_LIMIT = 12;
    private static final int FALLBACK_TEXT_MAX_LENGTH = 100;
    private static final int MIN_KEYWORD_LENGTH = 3;
    private static final int MAX_INDIVIDUAL_KEYWORDS = 8;
    private static final int CREDITS_RESULT_LIMIT = 20;
    private static final int YEAR_TOLERANCE = 2;

    private final GroqService groqService;
    private final TmdbService tmdbService;
    private final MovieScoringService scoringService;

    public AiController(GroqService groqService, TmdbService tmdbService, MovieScoringService scoringService) {
        this.groqService = groqService;
        this.tmdbService = tmdbService;
        this.scoringService = scoringService;
    }

    @PostMapping("/parse")
    public ResponseEntity<AiSearchResponse> parse(
            @Valid @RequestBody AiParseRequest request,
            @RequestParam(defaultValue = "fr-FR") String lang) {
        String userText = request.text().trim();
        String requestedMediaType = request.mediaType() != null ? request.mediaType() : "all";
        log.info("AI parse request: '{}' (lang={}, mediaType={})", userText, lang, requestedMediaType);

        // 1. LLM structured extraction
        AiMovieQuery parsed = groqService.parseUserQuery(userText);

        // 2. Unknown intent → empty
        if ("unknown".equals(parsed.getIntent())) {
            log.info("Intent=unknown, returning empty results");
            return ResponseEntity.ok(new AiSearchResponse(parsed, null, List.of(), List.of(), List.of(), 0));
        }

        // 3. Determine effective media type
        String effectiveType = resolveEffectiveType(requestedMediaType, parsed);
        log.info("Effective media type: {} (requested={}, llm={})", effectiveType, requestedMediaType, parsed.getType());

        // 4. Resolve results with enhanced fallback cascade
        EnhancedResult result = resolveResults(parsed, userText, lang, effectiveType);

        log.info("AI search: bestMatch={}, similar={}, results={} (intent={}, type={})",
                result.bestMatch != null ? result.bestMatch.title() : "none",
                result.similarMovies.size(),
                result.results.size(),
                parsed.getIntent(),
                effectiveType);

        return ResponseEntity.ok(new AiSearchResponse(
                parsed, result.bestMatch, result.suggestions, result.similarMovies, result.results, result.total));
    }

    /**
     * Determines the effective media type for search.
     * User's explicit choice overrides LLM detection.
     */
    private String resolveEffectiveType(String requested, AiMovieQuery parsed) {
        if ("movie".equals(requested) || "tv".equals(requested)) {
            return requested;
        }
        if (parsed.getType() != null) {
            return parsed.getType();
        }
        return "all";
    }

    // ============================
    // Enhanced resolution cascade
    // ============================

    private record EnhancedResult(MovieDto bestMatch, List<MovieDto> suggestions,
                                   List<MovieDto> similarMovies,
                                   List<MovieDto> results, int total) {}

    /**
     * Two-phase resolution:
     * Phase A: Find the exact movie/show (best match) via cascade steps
     * Phase B: Get similar movies/shows if best match found
     */
    private EnhancedResult resolveResults(AiMovieQuery parsed, String userText, String lang, String mediaType) {
        MovieDto bestMatch = null;
        List<MovieDto> results = List.of();

        // === PHASE A: Cascade search steps ===

        // Step 1: Title search
        CascadeHit hit = tryTitleSearch(parsed, lang, mediaType);
        if (hit != null) { bestMatch = hit.best; results = hit.results; }

        // Step 2: Alternate titles
        if (bestMatch == null) {
            hit = tryAlternateTitles(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 2b: Search queries
        if (bestMatch == null) {
            hit = trySearchQueries(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 3: Actor-based search
        if (bestMatch == null) {
            hit = tryActorSearch(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 3b: Director-based search
        if (bestMatch == null) {
            hit = tryDirectorSearch(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 4: Query-based search
        if (bestMatch == null) {
            hit = tryQuerySearch(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 5: Keyword-based search
        if (bestMatch == null) {
            hit = tryKeywordSearch(parsed, lang, mediaType);
            if (hit != null) { bestMatch = hit.best; results = hit.results; }
        }

        // Step 6: Discover with filters
        if (results.isEmpty() && hasFilters(parsed)) {
            log.info("Step 6: discover with filters genres={}, year={}, lang={}",
                    parsed.getGenres(), parsed.getYear(), parsed.getLanguage());
            SearchResult r = discoverByMediaType(parsed, mediaType, lang);
            if (!r.movies.isEmpty()) { results = r.movies; }
        }

        // Step 7: Raw user text fallback
        if (results.isEmpty()) {
            String truncated = userText.length() > FALLBACK_TEXT_MAX_LENGTH
                    ? userText.substring(0, FALLBACK_TEXT_MAX_LENGTH).replaceAll("\\s+\\S*$", "")
                    : userText;
            log.info("Step 7: fallback search with truncated userText ({}→{} chars)",
                    userText.length(), truncated.length());
            SearchResult r = searchByMediaType(truncated, mediaType, lang);
            results = r.movies;
        }

        // Step 8: Popular discover in genre fallback
        if (results.isEmpty() && parsed.getGenres() != null && !parsed.getGenres().isEmpty()) {
            log.info("Step 8: fallback discover popular in genre");
            SearchResult r = discoverByMediaType(parsed, mediaType, lang);
            results = r.movies;
        }

        // Tag bestMatch with media type if not already set
        if (bestMatch != null) {
            String bestType = "tv".equals(mediaType) ? "tv" : ("movie".equals(mediaType) ? "movie" : bestMatch.media_type());
            bestMatch = tagSingleMediaType(bestMatch, bestType != null ? bestType : "movie");
        }

        // Pick top suggestions from the same result pool (scored & ranked)
        List<MovieDto> suggestions = scoringService.pickSuggestions(results, bestMatch, parsed, mediaType);

        // === PHASE B: Get similar movies/shows ===
        List<MovieDto> similarMovies = fetchSimilar(bestMatch, mediaType, lang);

        // Remove bestMatch and suggestions from results to avoid duplicate display
        if (bestMatch != null || !suggestions.isEmpty()) {
            Set<Long> excludeIds = new HashSet<>();
            if (bestMatch != null) excludeIds.add(bestMatch.id());
            suggestions.stream().map(MovieDto::id).forEach(excludeIds::add);
            results = results.stream().filter(m -> !excludeIds.contains(m.id())).toList();
        }

        int total = results.size() + suggestions.size() + (bestMatch != null ? 1 : 0);
        return new EnhancedResult(bestMatch, suggestions, similarMovies, results, total);
    }

    // ============================
    // Cascade step methods
    // ============================

    private record CascadeHit(MovieDto best, List<MovieDto> results) {}

    private CascadeHit tryTitleSearch(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getTitle() == null || parsed.getTitle().isBlank()) return null;

        log.info("Step 1: searching by title '{}'", parsed.getTitle());
        SearchResult r = searchByMediaType(parsed.getTitle(), mediaType, lang);
        if (!r.movies.isEmpty()) {
            return new CascadeHit(scoringService.pickBestMatch(r.movies, parsed, mediaType), r.movies);
        }

        // Step 1b: title + year for disambiguation
        if (parsed.getYear() != null) {
            log.info("Step 1b: searching by title + year '{} {}'", parsed.getTitle(), parsed.getYear());
            r = searchByMediaType(parsed.getTitle() + " " + parsed.getYear(), mediaType, lang);
            if (!r.movies.isEmpty()) {
                return new CascadeHit(scoringService.pickBestMatch(r.movies, parsed, mediaType), r.movies);
            }
        }
        return null;
    }

    private CascadeHit tryAlternateTitles(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getAlternateTitles() == null) return null;
        for (String altTitle : parsed.getAlternateTitles()) {
            log.info("Step 2: trying alternate title '{}'", altTitle);
            SearchResult r = searchByMediaType(altTitle, mediaType, lang);
            if (!r.movies.isEmpty()) {
                return new CascadeHit(scoringService.pickBestMatch(r.movies, parsed, mediaType), r.movies);
            }
        }
        return null;
    }

    private CascadeHit trySearchQueries(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getSearchQueries() == null || parsed.getSearchQueries().isEmpty()) return null;

        List<MovieDto> allResults = new ArrayList<>();
        for (String searchQuery : parsed.getSearchQueries()) {
            log.info("Step 2b: trying search query '{}'", searchQuery);
            SearchResult r = searchByMediaType(searchQuery, mediaType, lang);
            if (!r.movies.isEmpty()) {
                allResults.addAll(r.movies);
            }
        }
        if (allResults.isEmpty()) return null;

        // Deduplicate by ID
        List<MovieDto> deduped = allResults.stream()
                .filter(m -> m.id() != null)
                .collect(Collectors.toMap(MovieDto::id, m -> m, (a, b) -> a))
                .values().stream().toList();
        return new CascadeHit(scoringService.pickBestMatch(deduped, parsed, mediaType), deduped);
    }

    private CascadeHit tryActorSearch(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getActors() == null || parsed.getActors().isEmpty()) return null;

        for (String actor : parsed.getActors()) {
            log.info("Step 3: searching by actor '{}'", actor);
            MovieDto best = searchByActor(actor, parsed, lang, mediaType);
            if (best != null) {
                SearchResult r = searchByMediaType(best.title(), mediaType, lang);
                List<MovieDto> results = r.movies.isEmpty() ? List.of(best) : r.movies;
                return new CascadeHit(best, results);
            }
        }
        return null;
    }

    private CascadeHit tryDirectorSearch(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getDirectors() == null || parsed.getDirectors().isEmpty()) return null;

        log.info("Step 3b: searching by director '{}'", parsed.getDirectors().getFirst());
        MovieDto best = searchByDirector(parsed, lang, mediaType);
        if (best != null) {
            SearchResult r = searchByMediaType(best.title(), mediaType, lang);
            List<MovieDto> results = r.movies.isEmpty() ? List.of(best) : r.movies;
            return new CascadeHit(best, results);
        }
        return null;
    }

    private CascadeHit tryQuerySearch(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getQuery() == null || parsed.getQuery().isBlank()) return null;

        log.info("Step 4: searching by query '{}'", parsed.getQuery());
        SearchResult r = searchByMediaType(parsed.getQuery(), mediaType, lang);
        if (!r.movies.isEmpty()) {
            return new CascadeHit(scoringService.pickBestMatch(r.movies, parsed, mediaType), r.movies);
        }
        return null;
    }

    private CascadeHit tryKeywordSearch(AiMovieQuery parsed, String lang, String mediaType) {
        if (parsed.getKeywords() == null || parsed.getKeywords().isEmpty()) return null;

        List<String> kws = parsed.getKeywords();
        log.info("Step 5: searching by keywords {}", kws);

        // 5a: All keywords combined
        String combined = String.join(" ", kws);
        SearchResult r = searchByMediaType(combined, mediaType, lang);
        if (!r.movies.isEmpty()) {
            return new CascadeHit(scoringService.pickBestMatch(r.movies, parsed, mediaType), r.movies);
        }

        // 5b: Keyword pairs
        CascadeHit pairHit = tryKeywordPairs(kws, parsed, lang, mediaType);
        if (pairHit != null) return pairHit;

        // 5b2: Keyword triplets
        CascadeHit tripletHit = tryKeywordTriplets(kws, parsed, lang, mediaType);
        if (tripletHit != null) return tripletHit;

        // 5c: Individual keywords fallback
        SearchResult kwResult = searchByKeywords(combined, mediaType, lang);
        if (!kwResult.movies.isEmpty()) {
            return new CascadeHit(scoringService.pickBestMatch(kwResult.movies, parsed, mediaType), kwResult.movies);
        }
        return null;
    }

    private CascadeHit tryKeywordPairs(List<String> kws, AiMovieQuery parsed, String lang, String mediaType) {
        if (kws.size() < 2) return null;
        int pairsChecked = 0;
        for (int i = 0; i < kws.size() - 1 && pairsChecked < MAX_KEYWORD_PAIRS; i++) {
            for (int j = i + 1; j < kws.size() && pairsChecked < MAX_KEYWORD_PAIRS; j++) {
                pairsChecked++;
                String pair = kws.get(i) + " " + kws.get(j);
                log.info("  Step 5b: keyword pair '{}'", pair);
                SearchResult pr = searchByMediaType(pair, mediaType, lang);
                if (!pr.movies.isEmpty()) {
                    return new CascadeHit(scoringService.pickBestMatch(pr.movies, parsed, mediaType), pr.movies);
                }
            }
        }
        return null;
    }

    private CascadeHit tryKeywordTriplets(List<String> kws, AiMovieQuery parsed, String lang, String mediaType) {
        if (kws.size() < 3) return null;
        int tripletsChecked = 0;
        for (int i = 0; i < kws.size() - 2 && tripletsChecked < MAX_KEYWORD_TRIPLETS; i++) {
            for (int j = i + 1; j < kws.size() - 1 && tripletsChecked < MAX_KEYWORD_TRIPLETS; j++) {
                for (int k = j + 1; k < kws.size() && tripletsChecked < MAX_KEYWORD_TRIPLETS; k++) {
                    tripletsChecked++;
                    String triplet = kws.get(i) + " " + kws.get(j) + " " + kws.get(k);
                    log.info("  Step 5b2: keyword triplet '{}'", triplet);
                    SearchResult tr = searchByMediaType(triplet, mediaType, lang);
                    if (!tr.movies.isEmpty()) {
                        return new CascadeHit(scoringService.pickBestMatch(tr.movies, parsed, mediaType), tr.movies);
                    }
                }
            }
        }
        return null;
    }

    private List<MovieDto> fetchSimilar(MovieDto bestMatch, String mediaType, String lang) {
        if (bestMatch == null || bestMatch.id() == null) return List.of();
        log.info("Phase B: fetching similar for '{}' (media_type={})", bestMatch.title(), bestMatch.media_type());
        try {
            MovieListResponse similar;
            String similarType;
            if ("tv".equals(bestMatch.media_type()) || "tv".equals(mediaType)) {
                similar = tmdbService.getSimilarTv(bestMatch.id(), 1, lang);
                similarType = "tv";
            } else {
                similar = tmdbService.getSimilarMovies(bestMatch.id(), 1, lang);
                similarType = "movie";
            }
            if (similar != null && similar.results() != null) {
                return tagMediaType(similar.results().stream().limit(SIMILAR_MOVIES_LIMIT).toList(), similarType);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch similar: {}", e.getMessage());
        }
        return List.of();
    }

    // ============================
    // Media-type aware search
    // ============================

    private record SearchResult(List<MovieDto> movies, int total) {}

    @FunctionalInterface
    private interface PageFetcher {
        MovieListResponse fetch(int page);
    }

    /**
     * Dispatches search to the correct TMDB endpoint based on media type.
     * Fetches page 1 + page 2 for a deeper result pool.
     */
    private SearchResult searchByMediaType(String query, String mediaType, String lang) {
        if ("tv".equals(mediaType)) {
            return fetchTwoPages(p -> tmdbService.searchTv(query, p, lang), "tv", "TV search", query);
        } else if ("movie".equals(mediaType)) {
            return fetchTwoPages(p -> tmdbService.searchMovies(query, p, lang), "movie", "movie search", query);
        } else {
            return fetchTwoPagesMulti(query, lang);
        }
    }

    private SearchResult fetchTwoPages(PageFetcher fetcher, String type, String label, String query) {
        MovieListResponse p1 = fetcher.fetch(1);
        if (p1 != null && p1.results() != null && !p1.results().isEmpty()) {
            List<MovieDto> all = new ArrayList<>(tagMediaType(p1.results(), type));
            if (p1.total_pages() != null && p1.total_pages() > 1) {
                try {
                    MovieListResponse p2 = fetcher.fetch(2);
                    if (p2 != null && p2.results() != null) {
                        all.addAll(tagMediaType(p2.results(), type));
                    }
                } catch (Exception e) {
                    log.debug("Failed to fetch {} page 2 for '{}': {}", label, query, e.getMessage());
                }
            }
            return new SearchResult(all, p1.total_results() != null ? p1.total_results() : all.size());
        }
        return new SearchResult(List.of(), 0);
    }

    private SearchResult fetchTwoPagesMulti(String query, String lang) {
        MovieListResponse p1 = tmdbService.searchMulti(query, 1, lang);
        if (p1 != null && p1.results() != null) {
            List<MovieDto> all = new ArrayList<>(p1.results().stream()
                    .filter(m -> !"person".equals(m.media_type())).toList());
            if (p1.total_pages() != null && p1.total_pages() > 1) {
                try {
                    MovieListResponse p2 = tmdbService.searchMulti(query, 2, lang);
                    if (p2 != null && p2.results() != null) {
                        all.addAll(p2.results().stream()
                                .filter(m -> !"person".equals(m.media_type())).toList());
                    }
                } catch (Exception e) {
                    log.debug("Failed to fetch multi search page 2 for '{}': {}", query, e.getMessage());
                }
            }
            return new SearchResult(all, all.size());
        }
        return new SearchResult(List.of(), 0);
    }

    /**
     * Dispatches discover to the correct TMDB endpoint based on media type.
     */
    private SearchResult discoverByMediaType(AiMovieQuery parsed, String mediaType, String lang) {
        if ("tv".equals(mediaType)) {
            return discoverTv(parsed, lang);
        } else if ("movie".equals(mediaType)) {
            return discoverMovie(parsed, lang);
        } else {
            SearchResult movieResult = discoverMovie(parsed, lang);
            SearchResult tvResult = discoverTv(parsed, lang);
            List<MovieDto> combined = Stream.concat(movieResult.movies.stream(), tvResult.movies.stream())
                    .sorted(Comparator.comparingDouble((MovieDto m) ->
                            m.popularity() != null ? m.popularity() : 0).reversed())
                    .limit(20)
                    .toList();
            return new SearchResult(combined, combined.size());
        }
    }

    private SearchResult discoverMovie(AiMovieQuery parsed, String lang) {
        Map<String, Integer> genreMap = scoringService.getGenreMap("movie");
        String genreIds = scoringService.resolveGenreIds(parsed.getGenres(), genreMap);
        MovieListResponse response = tmdbService.discoverMoviesWithGenreIds(
                genreIds,
                parsed.getYear(),
                scoringService.resolveSortRating(parsed.getSort()),
                scoringService.resolveLanguageCode(parsed.getLanguage()),
                scoringService.resolveTmdbSort(parsed.getSort(), "movie"),
                1, lang);
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            List<MovieDto> tagged = tagMediaType(response.results(), "movie");
            return new SearchResult(tagged, tagged.size());
        }
        return new SearchResult(List.of(), 0);
    }

    private SearchResult discoverTv(AiMovieQuery parsed, String lang) {
        Map<String, Integer> genreMap = scoringService.getGenreMap("tv");
        String genreIds = scoringService.resolveGenreIds(parsed.getGenres(), genreMap);
        MovieListResponse response = tmdbService.discoverTvWithGenreIds(
                genreIds,
                parsed.getYear(),
                scoringService.resolveSortRating(parsed.getSort()),
                scoringService.resolveLanguageCode(parsed.getLanguage()),
                scoringService.resolveTmdbSort(parsed.getSort(), "tv"),
                1, lang);
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            List<MovieDto> tagged = tagMediaType(response.results(), "tv");
            return new SearchResult(tagged, tagged.size());
        }
        return new SearchResult(List.of(), 0);
    }

    // ============================
    // Actor-based search
    // ============================

    private MovieDto searchByActor(String actorName, AiMovieQuery parsed, String lang, String mediaType) {
        try {
            PersonSearchResponse personSearch = tmdbService.searchPersons(actorName, 1, lang);
            if (personSearch == null || personSearch.results() == null || personSearch.results().isEmpty()) {
                return null;
            }

            Long personId = personSearch.results().getFirst().id();
            List<MovieDto> allCredits = new ArrayList<>();

            if (!"tv".equals(mediaType)) {
                PersonCreditsResponse movieCredits = tmdbService.getPersonMovies(personId, lang);
                if (movieCredits != null && movieCredits.cast() != null) {
                    allCredits.addAll(tagMediaType(movieCredits.cast().stream()
                            .filter(m -> m.title() != null).toList(), "movie"));
                }
            }

            if (!"movie".equals(mediaType)) {
                PersonCreditsResponse tvCredits = tmdbService.getPersonTvShows(personId, lang);
                if (tvCredits != null && tvCredits.cast() != null) {
                    allCredits.addAll(tagMediaType(tvCredits.cast().stream()
                            .filter(m -> m.title() != null).toList(), "tv"));
                }
            }

            List<MovieDto> filtered = filterCreditsByParsedQuery(allCredits, parsed, mediaType);
            if (!filtered.isEmpty()) {
                log.info("  Actor search found {} results, best: '{}'", filtered.size(), filtered.getFirst().title());
                return filtered.getFirst();
            }
        } catch (Exception e) {
            log.warn("Actor search failed: {}", e.getMessage());
        }
        return null;
    }

    // ============================
    // Director-based search
    // ============================

    private MovieDto searchByDirector(AiMovieQuery parsed, String lang, String mediaType) {
        try {
            String directorName = parsed.getDirectors().getFirst();
            PersonSearchResponse personSearch = tmdbService.searchPersons(directorName, 1, lang);
            if (personSearch == null || personSearch.results() == null || personSearch.results().isEmpty()) {
                return null;
            }

            Long personId = personSearch.results().getFirst().id();
            List<MovieDto> allCredits = new ArrayList<>();

            if (!"tv".equals(mediaType)) {
                PersonCreditsResponse movieCredits = tmdbService.getPersonMovies(personId, lang);
                if (movieCredits != null && movieCredits.crew() != null) {
                    allCredits.addAll(tagMediaType(movieCredits.crew().stream()
                            .filter(m -> m.title() != null)
                            .filter(m -> "Director".equalsIgnoreCase(m.job()))
                            .toList(), "movie"));
                }
            }

            if (!"movie".equals(mediaType)) {
                PersonCreditsResponse tvCredits = tmdbService.getPersonTvShows(personId, lang);
                if (tvCredits != null && tvCredits.crew() != null) {
                    allCredits.addAll(tagMediaType(tvCredits.crew().stream()
                            .filter(m -> m.title() != null)
                            .toList(), "tv"));
                }
            }

            List<MovieDto> filtered = filterCreditsByParsedQuery(allCredits, parsed, mediaType);
            if (!filtered.isEmpty()) {
                log.info("  Director search found {} results, best: '{}'", filtered.size(), filtered.getFirst().title());
                return filtered.getFirst();
            }
        } catch (Exception e) {
            log.warn("Director search failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Shared filtering logic for actor/director credit-based searches.
     * Filters by year tolerance and genre match, then sorts by popularity.
     */
    private List<MovieDto> filterCreditsByParsedQuery(List<MovieDto> credits, AiMovieQuery parsed, String mediaType) {
        Map<String, Integer> genreMap = scoringService.getGenreMap(mediaType);
        return credits.stream()
                .filter(m -> {
                    if (parsed.getYear() == null) return true;
                    if (m.release_date() == null || m.release_date().length() < 4) return false;
                    try {
                        int year = Integer.parseInt(m.release_date().substring(0, 4));
                        return Math.abs(year - parsed.getYear()) <= YEAR_TOLERANCE;
                    } catch (NumberFormatException e) { return false; }
                })
                .filter(m -> {
                    if (parsed.getGenres() == null || parsed.getGenres().isEmpty()) return true;
                    if (m.genre_ids() == null) return false;
                    return parsed.getGenres().stream().anyMatch(g -> {
                        Integer gid = genreMap.get(g.toLowerCase());
                        return gid != null && m.genre_ids().contains(gid);
                    });
                })
                .sorted(Comparator.comparingDouble((MovieDto m) ->
                        m.popularity() != null ? m.popularity() : 0).reversed())
                .limit(CREDITS_RESULT_LIMIT)
                .toList();
    }

    // ============================
    // Keyword search
    // ============================

    private SearchResult searchByKeywords(String query, String mediaType, String lang) {
        List<String> keywords = Arrays.stream(query.split("\\s+"))
                .filter(w -> w.length() >= MIN_KEYWORD_LENGTH)
                .sorted((a, b) -> b.length() - a.length())
                .limit(MAX_INDIVIDUAL_KEYWORDS)
                .toList();

        for (String keyword : keywords) {
            log.info("  keyword search: '{}'", keyword);
            SearchResult r = searchByMediaType(keyword, mediaType, lang);
            if (!r.movies.isEmpty()) return r;
        }
        return new SearchResult(List.of(), 0);
    }

    private boolean hasFilters(AiMovieQuery q) {
        return (q.getGenres() != null && !q.getGenres().isEmpty())
                || q.getYear() != null
                || q.getLanguage() != null
                || (q.getSort() != null && !"relevance".equals(q.getSort()));
    }

    // ============================
    // Media-type tagging helper
    // ============================

    private List<MovieDto> tagMediaType(List<MovieDto> movies, String type) {
        if (movies == null || movies.isEmpty()) return movies;
        return movies.stream()
                .map(m -> m.media_type() != null ? m : new MovieDto(
                        m.id(), m.title(), m.overview(), m.poster_path(),
                        m.backdrop_path(), m.release_date(), m.vote_average(), m.vote_count(),
                        m.popularity(), m.genre_ids(), m.original_language(), m.job(), type))
                .toList();
    }

    private MovieDto tagSingleMediaType(MovieDto m, String type) {
        if (m == null || m.media_type() != null) return m;
        return new MovieDto(m.id(), m.title(), m.overview(), m.poster_path(),
                m.backdrop_path(), m.release_date(), m.vote_average(), m.vote_count(),
                m.popularity(), m.genre_ids(), m.original_language(), m.job(), type);
    }
}
