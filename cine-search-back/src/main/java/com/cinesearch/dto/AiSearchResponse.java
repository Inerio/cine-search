package com.cinesearch.dto;

import java.util.List;

public record AiSearchResponse(
    AiMovieQuery parsed,
    List<MovieDto> results,
    int totalResults
) {}
