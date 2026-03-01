package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured output from the LLM query extraction.
 * Maps directly to the JSON schema defined in the system prompt.
 * Unknown JSON fields are ignored for resilience against LLM format variations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiMovieQuery {

    private String intent;       // "search" | "recommend" | "details" | "unknown"
    private String type;         // "movie" | "tv" | null
    private String query;        // free-text search query
    private String title;        // exact title identified by LLM
    private Integer year;        // 1888..2100
    private List<String> genres; // max 8
    private String language;
    private String country;
    private String platform;
    private String sort;         // "relevance" | "rating" | "popularity" | "recent" | null

    @JsonProperty("include_adult")
    private boolean includeAdult;

    // --- NEW fields for enhanced AI search ---
    private String confidence;                        // "high" | "medium" | "low" | null

    @JsonProperty("alternate_titles")
    private List<String> alternateTitles;             // max 6 alternate title guesses

    private List<String> actors;                       // max 5 actor names (English)
    private List<String> directors;                    // max 3 director names (English)
    private List<String> keywords;                     // max 15 thematic keywords (English)

    @JsonProperty("search_queries")
    private List<String> searchQueries;               // max 10 short TMDB-friendly queries

    private String explanation;                        // 1-2 sentences in user's language

    // --- Getters & Setters ---

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public boolean isIncludeAdult() { return includeAdult; }
    public void setIncludeAdult(boolean includeAdult) { this.includeAdult = includeAdult; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public List<String> getAlternateTitles() { return alternateTitles; }
    public void setAlternateTitles(List<String> alternateTitles) { this.alternateTitles = alternateTitles; }

    public List<String> getActors() { return actors; }
    public void setActors(List<String> actors) { this.actors = actors; }

    public List<String> getDirectors() { return directors; }
    public void setDirectors(List<String> directors) { this.directors = directors; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getSearchQueries() { return searchQueries; }
    public void setSearchQueries(List<String> searchQueries) { this.searchQueries = searchQueries; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    // --- Validation ---

    private static final List<String> VALID_INTENTS = List.of("search", "recommend", "details", "unknown");
    private static final List<String> VALID_TYPES = List.of("movie", "tv");
    private static final List<String> VALID_SORTS = List.of("relevance", "rating", "popularity", "recent");
    private static final List<String> VALID_CONFIDENCES = List.of("high", "medium", "low");

    /**
     * Validates and sanitizes all fields in-place.
     * Clamps values to allowed ranges and defaults invalid fields.
     */
    public void validateAndSanitize() {
        if (intent == null || !VALID_INTENTS.contains(intent)) {
            intent = "search";
        }
        if (type != null && !VALID_TYPES.contains(type)) {
            type = null;
        }
        if (year != null && (year < 1888 || year > 2100)) {
            year = null;
        }
        if (query != null && query.length() > 200) {
            query = query.substring(0, 200);
        }
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
        }
        if (genres != null && genres.size() > 8) {
            genres = genres.subList(0, 8);
        }
        if (sort != null && !VALID_SORTS.contains(sort)) {
            sort = null;
        }
        // --- Enhanced field validation ---
        if (confidence != null && !VALID_CONFIDENCES.contains(confidence)) {
            confidence = null;
        }
        if (alternateTitles != null && alternateTitles.size() > 6) {
            alternateTitles = alternateTitles.subList(0, 6);
        }
        if (actors != null && actors.size() > 5) {
            actors = actors.subList(0, 5);
        }
        if (directors != null && directors.size() > 3) {
            directors = directors.subList(0, 3);
        }
        if (keywords != null && keywords.size() > 15) {
            keywords = keywords.subList(0, 15);
        }
        if (searchQueries != null && searchQueries.size() > 10) {
            searchQueries = searchQueries.subList(0, 10);
        }
        if (searchQueries != null) {
            searchQueries = searchQueries.stream()
                    .filter(q -> q != null && !q.isBlank())
                    .map(q -> q.length() > 80 ? q.substring(0, 80) : q)
                    .toList();
        }
        if (explanation != null && explanation.length() > 500) {
            explanation = explanation.substring(0, 500);
        }
    }

    @Override
    public String toString() {
        return "AiMovieQuery{" +
                "intent='" + intent + '\'' +
                ", type='" + type + '\'' +
                ", query='" + query + '\'' +
                ", title='" + title + '\'' +
                ", year=" + year +
                ", genres=" + genres +
                ", language='" + language + '\'' +
                ", country='" + country + '\'' +
                ", confidence='" + confidence + '\'' +
                ", actors=" + actors +
                ", directors=" + directors +
                ", keywords=" + keywords +
                ", searchQueries=" + searchQueries +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
