package com.cinesearch.dto;

import java.util.List;

/**
 * TMDB genre list response for GET /genre/movie/list.
 */
public class GenreListResponse {

    private List<Genre> genres;

    public List<Genre> getGenres() { return genres; }
    public void setGenres(List<Genre> genres) { this.genres = genres; }

    /** Single genre entry with TMDB id and display name. */
    public static class Genre {
        private Integer id;
        private String name;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
