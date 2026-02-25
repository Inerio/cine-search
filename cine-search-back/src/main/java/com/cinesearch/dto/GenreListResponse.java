package com.cinesearch.dto;

import java.util.List;

public record GenreListResponse(
    List<Genre> genres
) {

    public record Genre(
        Integer id,
        String name
    ) {}
}
