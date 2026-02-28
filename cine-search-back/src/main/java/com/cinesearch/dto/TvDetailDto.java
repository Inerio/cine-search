package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TvDetailDto(
    Long id,
    String name,
    String overview,
    @JsonProperty("poster_path") String poster_path,
    @JsonProperty("backdrop_path") String backdrop_path,
    @JsonProperty("first_air_date") String first_air_date,
    @JsonProperty("last_air_date") String last_air_date,
    @JsonProperty("vote_average") Double vote_average,
    @JsonProperty("vote_count") Integer vote_count,
    @JsonProperty("episode_run_time") List<Integer> episode_run_time,
    @JsonProperty("number_of_seasons") Integer number_of_seasons,
    @JsonProperty("number_of_episodes") Integer number_of_episodes,
    String tagline,
    String status,
    List<GenreDto> genres,
    List<SeasonDto> seasons,
    List<NetworkDto> networks,
    @JsonProperty("created_by") List<CreatorDto> created_by,
    CreditsDto credits
) {

    public record GenreDto(
        Integer id,
        String name
    ) {}

    public record SeasonDto(
        Long id,
        String name,
        @JsonProperty("season_number") Integer season_number,
        @JsonProperty("episode_count") Integer episode_count,
        @JsonProperty("poster_path") String poster_path,
        @JsonProperty("air_date") String air_date,
        String overview
    ) {}

    public record NetworkDto(
        Integer id,
        String name,
        @JsonProperty("logo_path") String logo_path
    ) {}

    public record CreatorDto(
        Long id,
        String name,
        @JsonProperty("profile_path") String profile_path
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
