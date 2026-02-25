package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MovieDetailDto(
    Long id,
    String title,
    String overview,
    @JsonProperty("poster_path") String poster_path,
    @JsonProperty("backdrop_path") String backdrop_path,
    @JsonProperty("release_date") String release_date,
    @JsonProperty("vote_average") Double vote_average,
    @JsonProperty("vote_count") Integer vote_count,
    Integer runtime,
    String tagline,
    Long budget,
    Long revenue,
    String status,
    List<GenreDto> genres,
    CreditsDto credits
) {

    public record GenreDto(
        Integer id,
        String name
    ) {}

    public record CreditsDto(
        List<CastMemberDto> cast,
        List<CrewMemberDto> crew
    ) {}

    public record CastMemberDto(
        Long id,
        String name,
        String character,
        @JsonProperty("profile_path") String profile_path,
        Integer order
    ) {}

    public record CrewMemberDto(
        Long id,
        String name,
        String job,
        String department,
        @JsonProperty("profile_path") String profile_path
    ) {}
}
