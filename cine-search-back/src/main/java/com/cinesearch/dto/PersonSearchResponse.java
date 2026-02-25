package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PersonSearchResponse(
    Integer page,
    List<PersonDto> results,
    @JsonProperty("total_pages") Integer total_pages,
    @JsonProperty("total_results") Integer total_results
) {}
