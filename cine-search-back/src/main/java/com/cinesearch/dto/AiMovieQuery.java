package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured output from the LLM query extraction.
 * Maps directly to the JSON schema defined in the system prompt.
 * Unknown JSON fields are rejected to enforce strict schema compliance.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class AiMovieQuery {

    private String intent;       // "search" | "recommend" | "details" | "unknown"
    private String type;         // "movie" | "tv" | null
    private String query;        // free-text search query
    private String title;        // exact title identified by LLM
    private Integer year;        // 1888..2100
    private List<String> genres; // max 5
    private String language;
    private String country;
    private String platform;
    private String sort;         // "relevance" | "rating" | "popularity" | "recent" | null

    @JsonProperty("include_adult")
    private boolean includeAdult;

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

    // --- Validation ---

    private static final List<String> VALID_INTENTS = List.of("search", "recommend", "details", "unknown");
    private static final List<String> VALID_TYPES = List.of("movie", "tv");
    private static final List<String> VALID_SORTS = List.of("relevance", "rating", "popularity", "recent");

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
        if (query != null && query.length() > 120) {
            query = query.substring(0, 120);
        }
        if (title != null && title.length() > 120) {
            title = title.substring(0, 120);
        }
        if (genres != null && genres.size() > 5) {
            genres = genres.subList(0, 5);
        }
        if (sort != null && !VALID_SORTS.contains(sort)) {
            sort = null;
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
                ", platform='" + platform + '\'' +
                ", sort='" + sort + '\'' +
                ", includeAdult=" + includeAdult +
                '}';
    }
}
