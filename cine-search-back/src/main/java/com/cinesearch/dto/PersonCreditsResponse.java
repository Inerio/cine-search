package com.cinesearch.dto;

import java.util.List;

public record PersonCreditsResponse(
    Long id,
    List<MovieDto> cast,
    List<MovieDto> crew
) {}
