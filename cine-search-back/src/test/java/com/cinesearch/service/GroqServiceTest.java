package com.cinesearch.service;

import com.cinesearch.dto.AiMovieQuery;
import com.cinesearch.dto.GroqResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GroqService}.
 * Uses individual mock objects for each WebClient chain step and
 * ReflectionTestUtils to invoke private methods directly.
 */
@ExtendWith(MockitoExtension.class)
class GroqServiceTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GroqService groqService;

    @BeforeEach
    void setUp() {
        groqService = new GroqService(
                webClient,
                "test-api-key",
                "llama3-70b-8192",
                0.1,
                1024,
                ""  // empty override so init() reads from classpath
        );
        // Set a system prompt directly to avoid classpath dependency in most tests
        ReflectionTestUtils.setField(groqService, "systemPrompt", "You are a JSON extractor.");
    }

    /** Sets up the WebClient POST chain to return a Mono of the given response. */
    @SuppressWarnings("unchecked")
    private void mockPostChain(GroqResponse response) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GroqResponse.class))
                .thenReturn(response != null ? Mono.just(response) : Mono.empty());
    }

    /** Sets up the WebClient POST chain to throw an exception at retrieve(). */
    @SuppressWarnings("unchecked")
    private void mockPostChainThrows(RuntimeException ex) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(ex);
    }

    // ─── parseAndValidate (via reflection) ──────────────────────────────────

    @Test
    void validJson_shouldReturnParsedAiMovieQuery() {
        String json = """
                {
                  "intent": "search",
                  "type": "movie",
                  "query": "science fiction movies",
                  "year": 2024,
                  "genres": ["Science Fiction"],
                  "confidence": "high"
                }
                """;

        AiMovieQuery result = invokeParseAndValidate(json, "science fiction movies");

        assertEquals("search", result.getIntent());
        assertEquals("movie", result.getType());
        assertEquals("science fiction movies", result.getQuery());
        assertEquals(2024, result.getYear());
        assertEquals(List.of("Science Fiction"), result.getGenres());
        assertEquals("high", result.getConfidence());
    }

    @Test
    void jsonWrappedInMarkdownFences_shouldBeParsedCorrectly() {
        String raw = """
                ```json
                {
                  "intent": "recommend",
                  "type": "movie",
                  "query": "thriller movies",
                  "genres": ["Thriller"]
                }
                ```""";

        AiMovieQuery result = invokeParseAndValidate(raw, "thriller movies");

        assertEquals("recommend", result.getIntent());
        assertEquals("movie", result.getType());
        assertEquals("thriller movies", result.getQuery());
        assertEquals(List.of("Thriller"), result.getGenres());
    }

    @Test
    void jsonWithExtraTextBeforeAndAfter_shouldBeExtractedAndParsed() {
        String raw = """
                Here is the extracted query:
                {"intent": "details", "title": "Inception", "year": 2010, "type": "movie"}
                I hope this helps!
                """;

        AiMovieQuery result = invokeParseAndValidate(raw, "tell me about inception");

        assertEquals("details", result.getIntent());
        assertEquals("Inception", result.getTitle());
        assertEquals(2010, result.getYear());
        assertEquals("movie", result.getType());
    }

    @Test
    void completelyInvalidResponse_shouldReturnFallback() {
        String raw = "I'm sorry, I cannot understand your request. Please try again.";

        AiMovieQuery result = invokeParseAndValidate(raw, "find me a good movie");

        assertEquals("search", result.getIntent());
        assertEquals("find me a good movie", result.getQuery());
        assertFalse(result.isIncludeAdult());
    }

    @Test
    void jsonWithValidateAndSanitize_shouldClampInvalidFields() {
        String json = """
                {
                  "intent": "invalid_intent",
                  "type": "movie",
                  "query": "test",
                  "year": 1500
                }
                """;

        AiMovieQuery result = invokeParseAndValidate(json, "test");

        assertEquals("search", result.getIntent());
        assertNull(result.getYear(), "Year 1500 is below 1888 and should be nulled");
    }

    // ─── buildFallback (via reflection) ─────────────────────────────────────

    @Test
    void fallbackTruncatesLongTextTo120Chars() {
        String longText = "a".repeat(200);

        AiMovieQuery result = invokeBuildFallback(longText);

        assertEquals("search", result.getIntent());
        assertEquals(120, result.getQuery().length());
        assertFalse(result.isIncludeAdult());
    }

    @Test
    void fallbackPreservesShortText() {
        String shortText = "batman begins";

        AiMovieQuery result = invokeBuildFallback(shortText);

        assertEquals("search", result.getIntent());
        assertEquals("batman begins", result.getQuery());
        assertFalse(result.isIncludeAdult());
    }

    // ─── init() ─────────────────────────────────────────────────────────────

    @Test
    void init_shouldLoadSystemPromptFromClasspath() {
        GroqService freshService = new GroqService(
                webClient, "test-api-key", "llama3-70b-8192", 0.1, 1024, "");

        freshService.init();

        String loadedPrompt = (String) ReflectionTestUtils.getField(freshService, "systemPrompt");
        assertNotNull(loadedPrompt);
        assertFalse(loadedPrompt.isBlank(), "System prompt should not be blank after loading from classpath");
    }

    @Test
    void init_shouldUseEnvOverrideWhenProvided() {
        String customPrompt = "Custom system prompt for testing purposes.";
        GroqService freshService = new GroqService(
                webClient, "test-api-key", "llama3-70b-8192", 0.1, 1024, customPrompt);

        freshService.init();

        String loadedPrompt = (String) ReflectionTestUtils.getField(freshService, "systemPrompt");
        assertEquals(customPrompt, loadedPrompt);
    }

    // ─── parseUserQuery (full WebClient chain) ──────────────────────────────

    @Test
    void parseUserQuery_validResponse_shouldReturnParsedQuery() {
        String jsonContent = """
                {"intent": "search", "type": "movie", "query": "comedy films", "genres": ["Comedy"]}""";

        GroqResponse.Message message = new GroqResponse.Message("assistant", jsonContent);
        GroqResponse.Choice choice = new GroqResponse.Choice(message);
        GroqResponse groqResponse = new GroqResponse(List.of(choice));

        mockPostChain(groqResponse);

        AiMovieQuery result = groqService.parseUserQuery("I want comedy films");

        assertEquals("search", result.getIntent());
        assertEquals("movie", result.getType());
        assertEquals("comedy films", result.getQuery());
        assertEquals(List.of("Comedy"), result.getGenres());
    }

    @Test
    void parseUserQuery_nullResponse_shouldReturnFallback() {
        mockPostChain(null);

        AiMovieQuery result = groqService.parseUserQuery("some user text");

        assertEquals("search", result.getIntent());
        assertEquals("some user text", result.getQuery());
        assertFalse(result.isIncludeAdult());
    }

    @Test
    void parseUserQuery_emptyChoices_shouldReturnFallback() {
        GroqResponse groqResponse = new GroqResponse(List.of());
        mockPostChain(groqResponse);

        AiMovieQuery result = groqService.parseUserQuery("action movies");

        assertEquals("search", result.getIntent());
        assertEquals("action movies", result.getQuery());
    }

    @Test
    void parseUserQuery_webClientThrows_shouldReturnFallback() {
        mockPostChainThrows(new RuntimeException("Connection refused"));

        AiMovieQuery result = groqService.parseUserQuery("horror films");

        assertEquals("search", result.getIntent());
        assertEquals("horror films", result.getQuery());
        assertFalse(result.isIncludeAdult());
    }

    // ─── Helper methods ─────────────────────────────────────────────────────

    private AiMovieQuery invokeParseAndValidate(String raw, String originalUserText) {
        return (AiMovieQuery) ReflectionTestUtils.invokeMethod(
                groqService, "parseAndValidate", raw, originalUserText);
    }

    private AiMovieQuery invokeBuildFallback(String userText) {
        return (AiMovieQuery) ReflectionTestUtils.invokeMethod(
                groqService, "buildFallback", userText);
    }
}
