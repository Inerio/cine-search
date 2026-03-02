package com.cinesearch.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AiMovieQueryTest {

    @Test
    void emptyQuery_shouldHaveValidDefaults() {
        AiMovieQuery q = new AiMovieQuery();
        q.validateAndSanitize();
        assertEquals("search", q.getIntent());
        assertNull(q.getType());
        assertNull(q.getYear());
        assertNull(q.getGenres());
        assertNull(q.getKeywords());
        assertNull(q.getSearchQueries());
    }

    @Test
    void invalidIntent_shouldDefaultToSearch() {
        AiMovieQuery q = new AiMovieQuery();
        q.setIntent("invalid");
        q.validateAndSanitize();
        assertEquals("search", q.getIntent());
    }

    @Test
    void validIntents_shouldBePreserved() {
        for (String intent : List.of("search", "recommend", "details", "unknown")) {
            AiMovieQuery q = new AiMovieQuery();
            q.setIntent(intent);
            q.validateAndSanitize();
            assertEquals(intent, q.getIntent());
        }
    }

    @Test
    void invalidType_shouldBeNulled() {
        AiMovieQuery q = new AiMovieQuery();
        q.setType("cartoon");
        q.validateAndSanitize();
        assertNull(q.getType());
    }

    @Test
    void validTypes_shouldBePreserved() {
        for (String type : List.of("movie", "tv")) {
            AiMovieQuery q = new AiMovieQuery();
            q.setType(type);
            q.validateAndSanitize();
            assertEquals(type, q.getType());
        }
    }

    @Test
    void yearBelowMin_shouldBeNulled() {
        AiMovieQuery q = new AiMovieQuery();
        q.setYear(1800);
        q.validateAndSanitize();
        assertNull(q.getYear());
    }

    @Test
    void yearAboveMax_shouldBeNulled() {
        AiMovieQuery q = new AiMovieQuery();
        q.setYear(2200);
        q.validateAndSanitize();
        assertNull(q.getYear());
    }

    @Test
    void validYear_shouldBePreserved() {
        AiMovieQuery q = new AiMovieQuery();
        q.setYear(2024);
        q.validateAndSanitize();
        assertEquals(2024, q.getYear());
    }

    @Test
    void boundaryYears_shouldBePreserved() {
        AiMovieQuery q1 = new AiMovieQuery();
        q1.setYear(1888);
        q1.validateAndSanitize();
        assertEquals(1888, q1.getYear());

        AiMovieQuery q2 = new AiMovieQuery();
        q2.setYear(2100);
        q2.validateAndSanitize();
        assertEquals(2100, q2.getYear());
    }

    @Test
    void queryTruncatedTo200Chars() {
        AiMovieQuery q = new AiMovieQuery();
        q.setQuery("a".repeat(300));
        q.validateAndSanitize();
        assertEquals(200, q.getQuery().length());
    }

    @Test
    void titleTruncatedTo200Chars() {
        AiMovieQuery q = new AiMovieQuery();
        q.setTitle("t".repeat(250));
        q.validateAndSanitize();
        assertEquals(200, q.getTitle().length());
    }

    @Test
    void genresLimitedTo8() {
        AiMovieQuery q = new AiMovieQuery();
        q.setGenres(new ArrayList<>(IntStream.rangeClosed(1, 12).mapToObj(i -> "genre" + i).toList()));
        q.validateAndSanitize();
        assertEquals(8, q.getGenres().size());
    }

    @Test
    void actorsLimitedTo5() {
        AiMovieQuery q = new AiMovieQuery();
        q.setActors(new ArrayList<>(IntStream.rangeClosed(1, 8).mapToObj(i -> "actor" + i).toList()));
        q.validateAndSanitize();
        assertEquals(5, q.getActors().size());
    }

    @Test
    void directorsLimitedTo3() {
        AiMovieQuery q = new AiMovieQuery();
        q.setDirectors(new ArrayList<>(IntStream.rangeClosed(1, 5).mapToObj(i -> "dir" + i).toList()));
        q.validateAndSanitize();
        assertEquals(3, q.getDirectors().size());
    }

    @Test
    void keywordsLimitedTo15() {
        AiMovieQuery q = new AiMovieQuery();
        q.setKeywords(new ArrayList<>(IntStream.rangeClosed(1, 20).mapToObj(i -> "kw" + i).toList()));
        q.validateAndSanitize();
        assertEquals(15, q.getKeywords().size());
    }

    @Test
    void searchQueriesLimitedTo10() {
        AiMovieQuery q = new AiMovieQuery();
        q.setSearchQueries(new ArrayList<>(IntStream.rangeClosed(1, 15).mapToObj(i -> "sq" + i).toList()));
        q.validateAndSanitize();
        assertEquals(10, q.getSearchQueries().size());
    }

    @Test
    void searchQueriesTruncatedTo80Chars() {
        AiMovieQuery q = new AiMovieQuery();
        q.setSearchQueries(new ArrayList<>(List.of("a".repeat(100))));
        q.validateAndSanitize();
        assertEquals(80, q.getSearchQueries().getFirst().length());
    }

    @Test
    void searchQueriesBlanksFiltered() {
        AiMovieQuery q = new AiMovieQuery();
        q.setSearchQueries(new ArrayList<>(List.of("valid", "", "  ", "also valid")));
        q.validateAndSanitize();
        assertEquals(2, q.getSearchQueries().size());
        assertEquals("valid", q.getSearchQueries().get(0));
        assertEquals("also valid", q.getSearchQueries().get(1));
    }

    @Test
    void explanationTruncatedTo500Chars() {
        AiMovieQuery q = new AiMovieQuery();
        q.setExplanation("e".repeat(600));
        q.validateAndSanitize();
        assertEquals(500, q.getExplanation().length());
    }

    @Test
    void nullListsRemainNull() {
        AiMovieQuery q = new AiMovieQuery();
        q.validateAndSanitize();
        assertNull(q.getGenres());
        assertNull(q.getActors());
        assertNull(q.getDirectors());
        assertNull(q.getKeywords());
        assertNull(q.getSearchQueries());
        assertNull(q.getAlternateTitles());
    }

    @Test
    void invalidSort_shouldBeNulled() {
        AiMovieQuery q = new AiMovieQuery();
        q.setSort("alphabetical");
        q.validateAndSanitize();
        assertNull(q.getSort());
    }

    @Test
    void validSorts_shouldBePreserved() {
        for (String sort : List.of("relevance", "rating", "popularity", "recent")) {
            AiMovieQuery q = new AiMovieQuery();
            q.setSort(sort);
            q.validateAndSanitize();
            assertEquals(sort, q.getSort());
        }
    }

    @Test
    void invalidConfidence_shouldBeNulled() {
        AiMovieQuery q = new AiMovieQuery();
        q.setConfidence("very_high");
        q.validateAndSanitize();
        assertNull(q.getConfidence());
    }

    @Test
    void alternateTitlesLimitedTo6() {
        AiMovieQuery q = new AiMovieQuery();
        q.setAlternateTitles(new ArrayList<>(IntStream.rangeClosed(1, 10).mapToObj(i -> "title" + i).toList()));
        q.validateAndSanitize();
        assertEquals(6, q.getAlternateTitles().size());
    }

    @Test
    void toString_shouldNotThrow() {
        AiMovieQuery q = new AiMovieQuery();
        q.setIntent("search");
        q.setTitle("Test Movie");
        assertDoesNotThrow(q::toString);
        assertTrue(q.toString().contains("Test Movie"));
    }
}
