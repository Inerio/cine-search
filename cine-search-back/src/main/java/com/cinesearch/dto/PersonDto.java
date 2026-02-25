package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PersonDto(
    Long id,
    String name,
    @JsonProperty("profile_path") String profile_path,
    @JsonProperty("known_for_department") String known_for_department,
    Double popularity,
    Integer gender,
    @JsonProperty("known_for") List<MovieDto> known_for
) {}
