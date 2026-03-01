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
        String requestedMediaType = request.mediaType() != null ? request.mediaType() : "all";
        log.info("AI parse request: '{}' (lang={}, mediaType={})", userText, lang, requestedMediaType);

        // 1. LLM structured extraction
        AiMovieQuery parsed = groqService.parseUserQuery(userText);

        // 2. Unknown intent → empty
        if ("unknown".equals(parsed.getIntent())) {
            log.info("Intent=unknown, returning empty results");
            return ResponseEntity.ok(new AiSearchResponse(parsed, null, List.of(), List.of(), 0));
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
                parsed, result.bestMatch, result.similarMovies, result.results, result.total));
    }

    /**
     * Determines the effective media type for search.
     * User's explicit choice overrides LLM detection.
     */
    private String resolveEffectiveType(String requested, AiMovieQuery parsed) {
        if ("movie".equals(requested) || "tv".equals(requested)) {
            return requested;
        }
        // "all" → let LLM decide, or keep "all"
        if (parsed.getType() != null) {
            return parsed.getType();
        }
        return "all";
    }

    // ============================
    // Enhanced resolution cascade
    // ============================

    private record EnhancedResult(MovieDto bestMatch, List<MovieDto> similarMovies,
                                   List<MovieDto> results, int total) {}

    /**
     * Two-phase resolution:
     * Phase A: Find the exact movie/show (best match)
     * Phase B: Get similar movies/shows if best match found
     */
    private EnhancedResult resolveResults(AiMovieQuery parsed, String userText, String lang, String mediaType) {
        MovieDto bestMatch = null;
        List<MovieDto> results = List.of();

        // === PHASE A: Find the exact movie/show ===

        // Step 1: Title search (most precise)
        if (parsed.getTitle() != null && !parsed.getTitle().isBlank()) {
            log.info("Step 1: searching by title '{}'", parsed.getTitle());
            SearchResult r = searchByMediaType(parsed.getTitle(), mediaType, lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed, mediaType);
                results = r.movies;
            } else if (parsed.getYear() != null) {
                // Step 1b: title + year for disambiguation
                log.info("Step 1b: searching by title + year '{} {}'", parsed.getTitle(), parsed.getYear());
                r = searchByMediaType(parsed.getTitle() + " " + parsed.getYear(), mediaType, lang);
                if (!r.movies.isEmpty()) {
                    bestMatch = pickBestMatch(r.movies, parsed, mediaType);
                    results = r.movies;
                }
            }
        }

        // Step 2: Alternate titles
        if (bestMatch == null && parsed.getAlternateTitles() != null) {
            for (String altTitle : parsed.getAlternateTitles()) {
                log.info("Step 2: trying alternate title '{}'", altTitle);
                SearchResult r = searchByMediaType(altTitle, mediaType, lang);
                if (!r.movies.isEmpty()) {
                    bestMatch = pickBestMatch(r.movies, parsed, mediaType);
                    results = r.movies;
                    break;
                }
            }
        }

        // Step 2b: Search queries (LLM-generated TMDB-friendly short queries)
        // Try ALL queries and aggregate results for better best-match scoring
        if (bestMatch == null && parsed.getSearchQueries() != null && !parsed.getSearchQueries().isEmpty()) {
            List<MovieDto> allSearchQueryResults = new ArrayList<>();
            for (String searchQuery : parsed.getSearchQueries()) {
                log.info("Step 2b: trying search query '{}'", searchQuery);
                SearchResult r = searchByMediaType(searchQuery, mediaType, lang);
                if (!r.movies.isEmpty()) {
                    allSearchQueryResults.addAll(r.movies);
                }
            }
            if (!allSearchQueryResults.isEmpty()) {
                // Deduplicate by ID
                List<MovieDto> deduped = allSearchQueryResults.stream()
                        .filter(m -> m.id() != null)
                        .collect(Collectors.toMap(MovieDto::id, m -> m, (a, b) -> a))
                        .values().stream().toList();
                bestMatch = pickBestMatch(deduped, parsed, mediaType);
                results = deduped;
            }
        }

        // Step 3: Actor-based search (movie and TV) — try multiple actors
        if (bestMatch == null
                && parsed.getActors() != null && !parsed.getActors().isEmpty()) {
            for (String actor : parsed.getActors()) {
                log.info("Step 3: searching by actor '{}'", actor);
                // Temporarily set the first actor for searchByActor
                List<String> origActors = parsed.getActors();
                parsed.setActors(List.of(actor));
                bestMatch = searchByActor(parsed, lang, mediaType);
                parsed.setActors(origActors);
                if (bestMatch != null) {
                    if (results.isEmpty()) {
                        SearchResult r = searchByMediaType(bestMatch.title(), mediaType, lang);
                        results = r.movies.isEmpty() ? List.of(bestMatch) : r.movies;
                    }
                    break;
                }
            }
        }

        // Step 3b: Director-based search
        if (bestMatch == null && parsed.getDirectors() != null && !parsed.getDirectors().isEmpty()) {
            log.info("Step 3b: searching by director '{}'", parsed.getDirectors().getFirst());
            bestMatch = searchByDirector(parsed, lang, mediaType);
            if (bestMatch != null && results.isEmpty()) {
                SearchResult r = searchByMediaType(bestMatch.title(), mediaType, lang);
                results = r.movies.isEmpty() ? List.of(bestMatch) : r.movies;
            }
        }

        // Step 4: Query-based search
        if (bestMatch == null && parsed.getQuery() != null && !parsed.getQuery().isBlank()) {
            log.info("Step 4: searching by query '{}'", parsed.getQuery());
            SearchResult r = searchByMediaType(parsed.getQuery(), mediaType, lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed, mediaType);
                results = r.movies;
            }
        }

        // Step 5: Keyword-based search
        if (bestMatch == null && parsed.getKeywords() != null && !parsed.getKeywords().isEmpty()) {
            List<String> kws = parsed.getKeywords();
            log.info("Step 5: searching by keywords {}", kws);

            // 5a: All keywords combined
            String combined = String.join(" ", kws);
            SearchResult r = searchByMediaType(combined, mediaType, lang);
            if (!r.movies.isEmpty()) {
                bestMatch = pickBestMatch(r.movies, parsed, mediaType);
                results = r.movies;
            }

            // 5b: Keyword pairs (combinations of 2) — catches "Loulou Montmartre" type matches
            if (bestMatch == null && kws.size() >= 2) {
                int pairsChecked = 0;
                outer:
                for (int i = 0; i < kws.size() - 1 && pairsChecked < 20; i++) {
                    for (int j = i + 1; j < kws.size() && pairsChecked < 20; j++) {
                        pairsChecked++;
                        String pair = kws.get(i) + " " + kws.get(j);
                        log.info("  Step 5b: keyword pair '{}'", pair);
                        SearchResult pr = searchByMediaType(pair, mediaType, lang);
                        if (!pr.movies.isEmpty()) {
                            bestMatch = pickBestMatch(pr.movies, parsed, mediaType);
                            results = pr.movies;
                            break outer;
                        }
                    }
                }
            }

            // 5b2: Keyword triplets (combinations of 3) — more specific for niche searches
            if (bestMatch == null && kws.size() >= 3) {
                int tripletsChecked = 0;
                outer2:
                for (int i = 0; i < kws.size() - 2 && tripletsChecked < 10; i++) {
                    for (int j = i + 1; j < kws.size() - 1 && tripletsChecked < 10; j++) {
                        for (int k = j + 1; k < kws.size() && tripletsChecked < 10; k++) {
                            tripletsChecked++;
                            String triplet = kws.get(i) + " " + kws.get(j) + " " + kws.get(k);
                            log.info("  Step 5b2: keyword triplet '{}'", triplet);
                            SearchResult tr = searchByMediaType(triplet, mediaType, lang);
                            if (!tr.movies.isEmpty()) {
                                bestMatch = pickBestMatch(tr.movies, parsed, mediaType);
                                results = tr.movies;
                                break outer2;
                            }
                        }
                    }
                }
            }

            // 5c: Individual keywords (fallback)
            if (bestMatch == null) {
                SearchResult kwResult = searchByKeywords(combined, mediaType, lang);
                if (!kwResult.movies.isEmpty()) {
                    bestMatch = pickBestMatch(kwResult.movies, parsed, mediaType);
                    results = kwResult.movies;
                }
            }
        }

        // Step 6: Discover with filters (multi-genre)
        if (results.isEmpty() && hasFilters(parsed)) {
            log.info("Step 6: discover with filters genres={}, year={}, lang={}",
                    parsed.getGenres(), parsed.getYear(), parsed.getLanguage());
            SearchResult r = discoverByMediaType(parsed, mediaType, lang);
            if (!r.movies.isEmpty()) {
                results = r.movies;
            }
        }

        // Step 7: Raw user text fallback (truncated for TMDB compatibility)
        if (results.isEmpty()) {
            String truncated = userText.length() > 100
                    ? userText.substring(0, 100).replaceAll("\\s+\\S*$", "")
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

        // === PHASE B: Get similar movies/shows ===
        List<MovieDto> similarMovies = List.of();
        if (bestMatch != null && bestMatch.id() != null) {
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
                    similarMovies = tagMediaType(
                            similar.results().stream().limit(12).toList(), similarType);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch similar: {}", e.getMessage());
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
    // Media-type aware search
    // ============================

    private record SearchResult(List<MovieDto> movies, int total) {}

    /**
     * Dispatches search to the correct TMDB endpoint based on media type.
     * Fetches page 1 + page 2 for a deeper result pool.
     */
    private SearchResult searchByMediaType(String query, String mediaType, String lang) {
        if ("tv".equals(mediaType)) {
            MovieListResponse p1 = tmdbService.searchTv(query, 1, lang);
            if (p1 != null && p1.results() != null && !p1.results().isEmpty()) {
                List<MovieDto> all = new ArrayList<>(tagMediaType(p1.results(), "tv"));
                // Fetch page 2 for deeper pool
                if (p1.total_pages() != null && p1.total_pages() > 1) {
                    try {
                        MovieListResponse p2 = tmdbService.searchTv(query, 2, lang);
                        if (p2 != null && p2.results() != null) {
                            all.addAll(tagMediaType(p2.results(), "tv"));
                        }
                    } catch (Exception ignored) {}
                }
                return new SearchResult(all, p1.total_results() != null ? p1.total_results() : all.size());
            }
        } else if ("movie".equals(mediaType)) {
            MovieListResponse p1 = tmdbService.searchMovies(query, 1, lang);
            if (p1 != null && p1.results() != null && !p1.results().isEmpty()) {
                List<MovieDto> all = new ArrayList<>(tagMediaType(p1.results(), "movie"));
                if (p1.total_pages() != null && p1.total_pages() > 1) {
                    try {
                        MovieListResponse p2 = tmdbService.searchMovies(query, 2, lang);
                        if (p2 != null && p2.results() != null) {
                            all.addAll(tagMediaType(p2.results(), "movie"));
                        }
                    } catch (Exception ignored) {}
                }
                return new SearchResult(all, p1.total_results() != null ? p1.total_results() : all.size());
            }
        } else {
            // "all" → use multi search, filter out persons
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
                    } catch (Exception ignored) {}
                }
                return new SearchResult(all, all.size());
            }
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
            // "all" → try both, combine results
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
        String genreIds = resolveGenreIds(parsed.getGenres(), GENRE_MAP);
        MovieListResponse response = tmdbService.discoverMoviesWithGenreIds(
                genreIds,
                parsed.getYear(),
                resolveSortRating(parsed.getSort()),
                resolveLanguageCode(parsed.getLanguage()),
                resolveTmdbSort(parsed.getSort(), "movie"),
                1, lang);
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            List<MovieDto> tagged = tagMediaType(response.results(), "movie");
            return new SearchResult(tagged, tagged.size());
        }
        return new SearchResult(List.of(), 0);
    }

    private SearchResult discoverTv(AiMovieQuery parsed, String lang) {
        String genreIds = resolveGenreIds(parsed.getGenres(), TV_GENRE_MAP);
        MovieListResponse response = tmdbService.discoverTvWithGenreIds(
                genreIds,
                parsed.getYear(),
                resolveSortRating(parsed.getSort()),
                resolveLanguageCode(parsed.getLanguage()),
                resolveTmdbSort(parsed.getSort(), "tv"),
                1, lang);
        if (response != null && response.results() != null && !response.results().isEmpty()) {
            List<MovieDto> tagged = tagMediaType(response.results(), "tv");
            return new SearchResult(tagged, tagged.size());
        }
        return new SearchResult(List.of(), 0);
    }

    // ============================
    // Best match scoring
    // ============================

    private MovieDto pickBestMatch(List<MovieDto> results, AiMovieQuery parsed, String mediaType) {
        if (results.isEmpty()) return null;
        if (results.size() == 1) return results.getFirst();

        Map<String, Integer> genreMap = "tv".equals(mediaType) ? TV_GENRE_MAP : GENRE_MAP;

        MovieDto best = results.getFirst();
        int bestScore = -1;

        for (MovieDto movie : results) {
            int score = 0;

            // Title keyword matching (+15 per keyword found in movie title)
            if (parsed.getKeywords() != null && movie.title() != null) {
                String movieTitleLower = movie.title().toLowerCase();
                for (String kw : parsed.getKeywords()) {
                    if (kw.length() >= 3 && movieTitleLower.contains(kw.toLowerCase())) {
                        score += 15;
                    }
                }
            }

            // Search query exact title match (+25)
            if (parsed.getSearchQueries() != null && movie.title() != null) {
                String movieTitleLower = movie.title().toLowerCase();
                for (String sq : parsed.getSearchQueries()) {
                    if (movieTitleLower.equals(sq.toLowerCase()) ||
                        movieTitleLower.contains(sq.toLowerCase()) ||
                        sq.toLowerCase().contains(movieTitleLower)) {
                        score += 25;
                        break;
                    }
                }
            }

            // Parsed title match (+30 if title matches closely)
            if (parsed.getTitle() != null && movie.title() != null) {
                String parsedLower = parsed.getTitle().toLowerCase();
                String movieLower = movie.title().toLowerCase();
                if (movieLower.equals(parsedLower)) {
                    score += 30;
                } else if (movieLower.contains(parsedLower) || parsedLower.contains(movieLower)) {
                    score += 20;
                }
            }

            // Year match
            if (parsed.getYear() != null && movie.release_date() != null) {
                try {
                    int movieYear = Integer.parseInt(movie.release_date().substring(0, 4));
                    if (movieYear == parsed.getYear()) score += 10;
                    else if (Math.abs(movieYear - parsed.getYear()) <= 1) score += 5;
                } catch (Exception ignored) {}
            }

            // Genre overlap (+3 per matching genre)
            if (parsed.getGenres() != null && movie.genre_ids() != null) {
                for (String genre : parsed.getGenres()) {
                    Integer genreId = genreMap.get(genre.toLowerCase());
                    if (genreId != null && movie.genre_ids().contains(genreId)) {
                        score += 3;
                    }
                }
            }

            // Language match (+5)
            if (parsed.getLanguage() != null && movie.original_language() != null) {
                String langCode = resolveLanguageCode(parsed.getLanguage());
                if (langCode != null && langCode.equals(movie.original_language())) {
                    score += 5;
                }
            }

            // Popularity tie-breaker (+0 to +5)
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
    // Actor-based search
    // ============================

    private MovieDto searchByActor(AiMovieQuery parsed, String lang, String mediaType) {
        try {
            String actorName = parsed.getActors().getFirst();
            PersonSearchResponse personSearch = tmdbService.searchPersons(actorName, 1, lang);
            if (personSearch == null || personSearch.results() == null || personSearch.results().isEmpty()) {
                return null;
            }

            Long personId = personSearch.results().getFirst().id();
            List<MovieDto> allCredits = new ArrayList<>();

            // Get movie credits (unless media type is strictly "tv")
            if (!"tv".equals(mediaType)) {
                PersonCreditsResponse movieCredits = tmdbService.getPersonMovies(personId, lang);
                if (movieCredits != null && movieCredits.cast() != null) {
                    allCredits.addAll(tagMediaType(movieCredits.cast().stream()
                            .filter(m -> m.title() != null).toList(), "movie"));
                }
            }

            // Get TV credits (unless media type is strictly "movie")
            if (!"movie".equals(mediaType)) {
                PersonCreditsResponse tvCredits = tmdbService.getPersonTvShows(personId, lang);
                if (tvCredits != null && tvCredits.cast() != null) {
                    allCredits.addAll(tagMediaType(tvCredits.cast().stream()
                            .filter(m -> m.title() != null).toList(), "tv"));
                }
            }

            Map<String, Integer> genreMap = "tv".equals(mediaType) ? TV_GENRE_MAP : GENRE_MAP;

            List<MovieDto> filtered = allCredits.stream()
                    .filter(m -> {
                        if (parsed.getYear() == null) return true;
                        if (m.release_date() == null || m.release_date().length() < 4) return false;
                        try {
                            int year = Integer.parseInt(m.release_date().substring(0, 4));
                            return Math.abs(year - parsed.getYear()) <= 2;
                        } catch (Exception e) { return false; }
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
                    .limit(20)
                    .toList();

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

            // Get movie credits (crew — directed)
            if (!"tv".equals(mediaType)) {
                PersonCreditsResponse movieCredits = tmdbService.getPersonMovies(personId, lang);
                if (movieCredits != null && movieCredits.crew() != null) {
                    allCredits.addAll(tagMediaType(movieCredits.crew().stream()
                            .filter(m -> m.title() != null)
                            .filter(m -> "Director".equalsIgnoreCase(m.job()))
                            .toList(), "movie"));
                }
            }

            // Get TV credits (crew — created/directed)
            if (!"movie".equals(mediaType)) {
                PersonCreditsResponse tvCredits = tmdbService.getPersonTvShows(personId, lang);
                if (tvCredits != null && tvCredits.crew() != null) {
                    allCredits.addAll(tagMediaType(tvCredits.crew().stream()
                            .filter(m -> m.title() != null)
                            .toList(), "tv"));
                }
            }

            Map<String, Integer> genreMap = "tv".equals(mediaType) ? TV_GENRE_MAP : GENRE_MAP;

            List<MovieDto> filtered = allCredits.stream()
                    .filter(m -> {
                        if (parsed.getYear() == null) return true;
                        if (m.release_date() == null || m.release_date().length() < 4) return false;
                        try {
                            int year = Integer.parseInt(m.release_date().substring(0, 4));
                            return Math.abs(year - parsed.getYear()) <= 2;
                        } catch (Exception e) { return false; }
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
                    .limit(20)
                    .toList();

            if (!filtered.isEmpty()) {
                log.info("  Director search found {} results, best: '{}'", filtered.size(), filtered.getFirst().title());
                return filtered.getFirst();
            }
        } catch (Exception e) {
            log.warn("Director search failed: {}", e.getMessage());
        }
        return null;
    }

    // ============================
    // Keyword search
    // ============================

    private SearchResult searchByKeywords(String query, String mediaType, String lang) {
        List<String> keywords = Arrays.stream(query.split("\\s+"))
                .filter(w -> w.length() > 2)
                .sorted((a, b) -> b.length() - a.length())
                .limit(8)
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
    // Genre mapping (Movie + TV)
    // ============================

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

    private static final Map<String, Integer> TV_GENRE_MAP = Map.ofEntries(
            Map.entry("action", 10759), Map.entry("action & adventure", 10759),
            Map.entry("adventure", 10759), Map.entry("aventure", 10759),
            Map.entry("animation", 16),
            Map.entry("comedy", 35), Map.entry("comédie", 35), Map.entry("comedie", 35),
            Map.entry("crime", 80),
            Map.entry("documentary", 99), Map.entry("documentaire", 99),
            Map.entry("drama", 18), Map.entry("drame", 18),
            Map.entry("family", 10751), Map.entry("famille", 10751),
            Map.entry("fantasy", 10765), Map.entry("fantaisie", 10765), Map.entry("fantastique", 10765),
            Map.entry("sci-fi", 10765), Map.entry("science-fiction", 10765), Map.entry("science fiction", 10765), Map.entry("sf", 10765),
            Map.entry("history", 36), Map.entry("histoire", 36), Map.entry("historique", 36),
            Map.entry("horror", 27), Map.entry("horreur", 27),
            Map.entry("music", 10402), Map.entry("musique", 10402),
            Map.entry("mystery", 9648), Map.entry("mystère", 9648), Map.entry("mystere", 9648),
            Map.entry("romance", 10749),
            Map.entry("thriller", 53),
            Map.entry("war", 10768), Map.entry("guerre", 10768), Map.entry("war & politics", 10768),
            Map.entry("western", 37)
    );

    private String resolveGenreIds(List<String> genres, Map<String, Integer> genreMap) {
        if (genres == null || genres.isEmpty()) return null;
        String ids = genres.stream()
                .map(g -> genreMap.get(g.toLowerCase()))
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

    // ============================
    // Media-type tagging helper
    // ============================

    /**
     * Creates new MovieDto instances with the media_type field explicitly set.
     * Needed because TMDB /search/movie, /search/tv, and /discover endpoints
     * do NOT return media_type (only /search/multi does).
     */
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

    private String resolveTmdbSort(String sort, String mediaType) {
        if (sort == null) return null;
        return switch (sort) {
            case "rating" -> "vote_average.desc";
            case "popularity" -> "popularity.desc";
            case "recent" -> "tv".equals(mediaType) ? "first_air_date.desc" : "primary_release_date.desc";
            default -> null;
        };
    }
}
