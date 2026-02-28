package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MovieDto(
    Long id,
    @JsonAlias("name") String title,
    String overview,
    @JsonProperty("poster_path") String poster_path,
    @JsonProperty("backdrop_path") String backdrop_path,
    @JsonAlias("first_air_date") @JsonProperty("release_date") String release_date,
    @JsonProperty("vote_average") Double vote_average,
    @JsonProperty("vote_count") Integer vote_count,
    Double popularity,
    @JsonProperty("genre_ids") List<Integer> genre_ids,
    @JsonProperty("original_language") String original_language,
    String job,
    @JsonProperty("media_type") String media_type
) {}
