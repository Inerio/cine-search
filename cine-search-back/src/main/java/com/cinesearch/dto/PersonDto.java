package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PersonDto {
    private Long id;
    private String name;
    @JsonProperty("profile_path")
    private String profilePath;
    @JsonProperty("known_for_department")
    private String knownForDepartment;
    private Double popularity;
    private Integer gender;
    @JsonProperty("known_for")
    private List<MovieDto> knownFor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getKnownForDepartment() { return knownForDepartment; }
    public void setKnownForDepartment(String knownForDepartment) { this.knownForDepartment = knownForDepartment; }
    public Double getPopularity() { return popularity; }
    public void setPopularity(Double popularity) { this.popularity = popularity; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public List<MovieDto> getKnownFor() { return knownFor; }
    public void setKnownFor(List<MovieDto> knownFor) { this.knownFor = knownFor; }
}
