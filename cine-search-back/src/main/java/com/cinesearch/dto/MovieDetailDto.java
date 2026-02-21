package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MovieDetailDto {
    private Long id;
    private String title;
    private String overview;
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    @JsonProperty("release_date")
    private String releaseDate;
    @JsonProperty("vote_average")
    private Double voteAverage;
    @JsonProperty("vote_count")
    private Integer voteCount;
    private Integer runtime;
    private String tagline;
    private Long budget;
    private Long revenue;
    private String status;
    private List<GenreDto> genres;
    private CreditsDto credits;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    public Integer getVoteCount() { return voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public Long getBudget() { return budget; }
    public void setBudget(Long budget) { this.budget = budget; }
    public Long getRevenue() { return revenue; }
    public void setRevenue(Long revenue) { this.revenue = revenue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<GenreDto> getGenres() { return genres; }
    public void setGenres(List<GenreDto> genres) { this.genres = genres; }
    public CreditsDto getCredits() { return credits; }
    public void setCredits(CreditsDto credits) { this.credits = credits; }

    public static class GenreDto {
        private Integer id;
        private String name;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class CreditsDto {
        private List<CastMemberDto> cast;
        private List<CrewMemberDto> crew;

        public List<CastMemberDto> getCast() { return cast; }
        public void setCast(List<CastMemberDto> cast) { this.cast = cast; }
        public List<CrewMemberDto> getCrew() { return crew; }
        public void setCrew(List<CrewMemberDto> crew) { this.crew = crew; }
    }

    public static class CastMemberDto {
        private Long id;
        private String name;
        private String character;
        @JsonProperty("profile_path")
        private String profilePath;
        private Integer order;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCharacter() { return character; }
        public void setCharacter(String character) { this.character = character; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
    }

    public static class CrewMemberDto {
        private Long id;
        private String name;
        private String job;
        private String department;
        @JsonProperty("profile_path")
        private String profilePath;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getJob() { return job; }
        public void setJob(String job) { this.job = job; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    }
}
