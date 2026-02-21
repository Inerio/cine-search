package com.cinesearch.dto;

import java.util.List;

/**
 * Response for POST /api/ai/parse — combines AI parsing + TMDB results.
 */
public class AiSearchResponse {

    private AiMovieQuery parsed;
    private List<MovieDto> results;
    private int totalResults;

    public AiSearchResponse() {}

    public AiSearchResponse(AiMovieQuery parsed, List<MovieDto> results, int totalResults) {
        this.parsed = parsed;
        this.results = results;
        this.totalResults = totalResults;
    }

    public AiMovieQuery getParsed() { return parsed; }
    public void setParsed(AiMovieQuery parsed) { this.parsed = parsed; }

    public List<MovieDto> getResults() { return results; }
    public void setResults(List<MovieDto> results) { this.results = results; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
}
