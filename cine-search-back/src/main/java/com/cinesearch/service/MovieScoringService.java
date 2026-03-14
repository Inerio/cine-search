package com.cinesearch.service;

import com.cinesearch.dto.AiMovieQuery;
import com.cinesearch.dto.MovieDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Scores movies against parsed AI queries for best-match ranking.
 * Extracted from AiController for single responsibility and testability.
 */
@Service
public class MovieScoringService {

    private static final Logger log = LoggerFactory.getLogger(MovieScoringService.class);

    // Scoring weights
    private static final int SCORE_KEYWORD_IN_TITLE = 15;
    private static final int SCORE_SEARCH_QUERY_MATCH = 25;
    private static final int SCORE_TITLE_EXACT = 30;
    private static final int SCORE_TITLE_PARTIAL = 20;
    private static final int SCORE_YEAR_EXACT = 10;
    private static final int SCORE_YEAR_CLOSE = 5;
    private static final int SCORE_GENRE_MATCH = 3;
    private static final int SCORE_LANGUAGE_MATCH = 5;
    private static final double SCORE_POPULARITY_DIVISOR = 10.0;
    private static final int SCORE_POPULARITY_CAP = 5;
    private static final int MIN_KEYWORD_LENGTH = 3;
    private static final int TOP_SUGGESTIONS_LIMIT = 4;

    // Genre mapping (Movie)
    static final Map<String, Integer> GENRE_MAP = Map.ofEntries(
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

    // Genre mapping (TV)
    static final Map<String, Integer> TV_GENRE_MAP = Map.ofEntries(
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

    // Language ISO 639-1 mapping
    static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
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

    /**
     * Scores a single movie against the parsed AI query.
     * Higher score = better match.
     */
    public int scoreMovie(MovieDto movie, AiMovieQuery parsed, String mediaType) {
        int score = 0;
        Map<String, Integer> genreMap = "tv".equals(mediaType) ? TV_GENRE_MAP : GENRE_MAP;

        // Title keyword matching
        if (parsed.getKeywords() != null && movie.title() != null) {
            String movieTitleLower = movie.title().toLowerCase();
            for (String kw : parsed.getKeywords()) {
                if (kw.length() >= MIN_KEYWORD_LENGTH && movieTitleLower.contains(kw.toLowerCase())) {
                    score += SCORE_KEYWORD_IN_TITLE;
                }
            }
        }

        // Search query title match
        if (parsed.getSearchQueries() != null && movie.title() != null) {
            String movieTitleLower = movie.title().toLowerCase();
            for (String sq : parsed.getSearchQueries()) {
                if (movieTitleLower.equals(sq.toLowerCase()) ||
                    movieTitleLower.contains(sq.toLowerCase()) ||
                    sq.toLowerCase().contains(movieTitleLower)) {
                    score += SCORE_SEARCH_QUERY_MATCH;
                    break;
                }
            }
        }

        // Parsed title match
        if (parsed.getTitle() != null && movie.title() != null) {
            String parsedLower = parsed.getTitle().toLowerCase();
            String movieLower = movie.title().toLowerCase();
            if (movieLower.equals(parsedLower)) {
                score += SCORE_TITLE_EXACT;
            } else if (movieLower.contains(parsedLower) || parsedLower.contains(movieLower)) {
                score += SCORE_TITLE_PARTIAL;
            }
        }

        // Year match
        if (parsed.getYear() != null && movie.release_date() != null && movie.release_date().length() >= 4) {
            try {
                int movieYear = Integer.parseInt(movie.release_date().substring(0, 4));
                if (movieYear == parsed.getYear()) score += SCORE_YEAR_EXACT;
                else if (Math.abs(movieYear - parsed.getYear()) <= 1) score += SCORE_YEAR_CLOSE;
            } catch (NumberFormatException e) {
                log.debug("Could not parse year from release_date: {}", movie.release_date());
            }
        }

        // Genre overlap
        if (parsed.getGenres() != null && movie.genre_ids() != null) {
            for (String genre : parsed.getGenres()) {
                Integer genreId = genreMap.get(genre.toLowerCase());
                if (genreId != null && movie.genre_ids().contains(genreId)) {
                    score += SCORE_GENRE_MATCH;
                }
            }
        }

        // Language match
        if (parsed.getLanguage() != null && movie.original_language() != null) {
            String langCode = resolveLanguageCode(parsed.getLanguage());
            if (langCode != null && langCode.equals(movie.original_language())) {
                score += SCORE_LANGUAGE_MATCH;
            }
        }

        // Popularity tie-breaker
        if (movie.popularity() != null) {
            score += (int) Math.min(movie.popularity() / SCORE_POPULARITY_DIVISOR, SCORE_POPULARITY_CAP);
        }

        return score;
    }

    public MovieDto pickBestMatch(List<MovieDto> results, AiMovieQuery parsed, String mediaType) {
        if (results.isEmpty()) return null;
        if (results.size() == 1) return results.getFirst();
        return results.stream()
                .max(Comparator.comparingInt(m -> scoreMovie(m, parsed, mediaType)))
                .orElse(results.getFirst());
    }

    /**
     * Picks the top N suggestions (excluding bestMatch) ranked by score.
     */
    public List<MovieDto> pickSuggestions(List<MovieDto> candidates, MovieDto bestMatch,
                                          AiMovieQuery parsed, String mediaType) {
        if (candidates == null || candidates.size() <= 1 || bestMatch == null) return List.of();
        Long bestId = bestMatch.id();
        return candidates.stream()
                .filter(m -> !Objects.equals(m.id(), bestId))
                .sorted(Comparator.comparingInt((MovieDto m) -> scoreMovie(m, parsed, mediaType)).reversed())
                .limit(TOP_SUGGESTIONS_LIMIT)
                .toList();
    }

    public Map<String, Integer> getGenreMap(String mediaType) {
        return "tv".equals(mediaType) ? TV_GENRE_MAP : GENRE_MAP;
    }

    public String resolveGenreIds(List<String> genres, Map<String, Integer> genreMap) {
        if (genres == null || genres.isEmpty()) return null;
        String ids = genres.stream()
                .map(g -> genreMap.get(g.toLowerCase()))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return ids.isEmpty() ? null : ids;
    }

    public String resolveLanguageCode(String language) {
        if (language == null) return null;
        return LANGUAGE_MAP.get(language.toLowerCase());
    }

    public Double resolveSortRating(String sort) {
        if ("rating".equals(sort)) return 7.0;
        return null;
    }

    public String resolveTmdbSort(String sort, String mediaType) {
        if (sort == null) return null;
        return switch (sort) {
            case "rating" -> "vote_average.desc";
            case "popularity" -> "popularity.desc";
            case "recent" -> "tv".equals(mediaType) ? "first_air_date.desc" : "primary_release_date.desc";
            default -> null;
        };
    }
}
