package com.cinesearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class CineSearchApplication {

    private static final Logger log = LoggerFactory.getLogger(CineSearchApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CineSearchApplication.class, args);
    }

    @Bean
    CommandLineRunner validateApiKeys(@Value("${tmdb.api.key}") String tmdbKey,
                                      @Value("${groq.api.key}") String groqKey) {
        return args -> {
            if (tmdbKey == null || tmdbKey.isBlank()) {
                throw new IllegalStateException("TMDB_API_KEY is required. Set the TMDB_API_KEY environment variable.");
            }
            if (groqKey == null || groqKey.isBlank()) {
                throw new IllegalStateException("GROQ_API_KEY is required. Set the GROQ_API_KEY environment variable.");
            }
            log.info("API keys validated successfully");
        };
    }
}
