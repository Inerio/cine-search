package com.cinesearch.dto;

import java.util.List;

public record AiSearchResponse(
    AiMovieQuery parsed,
    MovieDto bestMatch,
    List<MovieDto> suggestions,
    List<MovieDto> similarMovies,
    List<MovieDto> results,
    int totalResults
) {}
