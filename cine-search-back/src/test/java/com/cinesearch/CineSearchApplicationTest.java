package com.cinesearch;

import com.cinesearch.service.GroqService;
import com.cinesearch.service.TmdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring application context loads successfully.
 * Uses the "test" profile which provides dummy API keys and disables caching.
 * Services that depend on external APIs are mocked to avoid real network calls.
 */
@SpringBootTest
@ActiveProfiles("test")
class CineSearchApplicationTest {

    @MockitoBean
    private TmdbService tmdbService;

    @MockitoBean
    private GroqService groqService;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // If the context fails to load, this test will fail automatically.
    }
}
