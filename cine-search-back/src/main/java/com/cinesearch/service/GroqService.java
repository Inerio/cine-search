package com.cinesearch.service;

import com.cinesearch.dto.AiMovieQuery;
import com.cinesearch.dto.GroqResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LLM integration service using Groq's OpenAI-compatible API.
 * Sends user queries to the LLM with a structured system prompt,
 * parses the JSON response into an {@link AiMovieQuery}, and falls
 * back to a plain search query on parse failure.
 */
@Service
public class GroqService {

    private static final Logger log = LoggerFactory.getLogger(GroqService.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final String systemPromptOverride;

    private String systemPrompt;

    private final ObjectMapper strictMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public GroqService(
            @Qualifier("groqWebClient") WebClient webClient,
            @Value("${groq.api.key}") String apiKey,
            @Value("${app.llm.model}") String model,
            @Value("${app.llm.temperature}") double temperature,
            @Value("${app.llm.max-tokens}") int maxTokens,
            @Value("${app.llm.system-prompt:}") String systemPromptOverride) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.systemPromptOverride = systemPromptOverride;
    }

    @PostConstruct
    void init() {
        // Priority: ENV override > local file
        if (systemPromptOverride != null && !systemPromptOverride.isBlank()) {
            this.systemPrompt = systemPromptOverride;
            log.info("LLM system prompt loaded from ENV override ({} chars)", systemPrompt.length());
        } else {
            try {
                ClassPathResource resource = new ClassPathResource("llm/system-prompt.txt");
                this.systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.info("LLM system prompt loaded from classpath ({} chars)", systemPrompt.length());
            } catch (IOException e) {
                log.error("Failed to load system prompt from classpath", e);
                this.systemPrompt = "You are a JSON extractor. Return valid JSON only.";
            }
        }
        // Log model info, never log key/prompt content
        log.info("Groq LLM ready — model={}, temp={}, maxTokens={}", model, temperature, maxTokens);
    }

    /**
     * Sends user text to Groq LLM and returns a validated AiMovieQuery.
     * Falls back to a basic search query if LLM response is unparseable.
     */
    public AiMovieQuery parseUserQuery(String userText) {
        String userMessage = "USER_QUERY: \"\"\"\n" + userText + "\n\"\"\"";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        try {
            log.info("Groq request — userText length={}", userText.length());

            GroqResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(3))
                            .filter(t -> t instanceof WebClientResponseException.TooManyRequests)
                            .doBeforeRetry(s -> log.warn("Groq 429 — retry {} ...", s.totalRetries() + 1)))
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String raw = response.getChoices().getFirst().getMessage().getContent().trim();
                log.info("Groq raw response: {}", raw);
                return parseAndValidate(raw, userText);
            }

        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage());
        }

        // Fallback: treat user text as plain search
        return buildFallback(userText);
    }

    private AiMovieQuery parseAndValidate(String raw, String originalUserText) {
        // Strip potential markdown code fences
        String json = raw.replaceAll("^```json\\s*", "").replaceAll("```$", "").trim();

        try {
            AiMovieQuery query = strictMapper.readValue(json, AiMovieQuery.class);
            query.validateAndSanitize();
            log.info("Parsed AI query: {}", query);
            return query;

        } catch (Exception e) {
            log.warn("JSON parse/validation failed ({}), using fallback", e.getMessage());
            return buildFallback(originalUserText);
        }
    }

    private AiMovieQuery buildFallback(String userText) {
        AiMovieQuery fallback = new AiMovieQuery();
        fallback.setIntent("search");
        fallback.setQuery(userText.length() > 120 ? userText.substring(0, 120) : userText);
        fallback.setIncludeAdult(false);
        log.info("Using fallback query: search for '{}'", fallback.getQuery());
        return fallback;
    }
}
