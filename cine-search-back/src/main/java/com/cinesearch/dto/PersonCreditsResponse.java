package com.cinesearch.dto;

import java.util.List;

public class PersonCreditsResponse {
    private Long id;
    private List<MovieDto> cast;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<MovieDto> getCast() { return cast; }
    public void setCast(List<MovieDto> cast) { this.cast = cast; }
}
