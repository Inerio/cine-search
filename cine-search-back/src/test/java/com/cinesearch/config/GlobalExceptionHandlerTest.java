package com.cinesearch.config;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the GlobalExceptionHandler using a standalone MockMvc setup
 * with a test controller that throws specific exception types.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * Minimal controller that throws specific exceptions to exercise the GlobalExceptionHandler.
     */
    @RestController
    static class TestExceptionController {

        @GetMapping("/test/webclient-error")
        public String throwWebClientError() {
            throw WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "resource not found".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );
        }

        @GetMapping("/test/webclient-error-502")
        public String throwWebClientBadGateway() {
            throw WebClientResponseException.create(
                    HttpStatus.BAD_GATEWAY.value(),
                    "Bad Gateway",
                    HttpHeaders.EMPTY,
                    "upstream error".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );
        }

        @GetMapping("/test/constraint-violation")
        public String throwConstraintViolation() {
            throw new ConstraintViolationException("page: must be greater than or equal to 1", Set.of());
        }

        @GetMapping("/test/generic-error")
        public String throwGenericException() {
            throw new RuntimeException("Something went wrong");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("WebClientResponseException 404 returns the upstream 404 status code")
    void handleWebClientError_returnsUpstreamStatus() throws Exception {
        mockMvc.perform(get("/test/webclient-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("External API error"))
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    @DisplayName("WebClientResponseException 502 returns the upstream 502 status code")
    void handleWebClientError_returnsBadGateway() throws Exception {
        mockMvc.perform(get("/test/webclient-error-502").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("External API error"))
                .andExpect(jsonPath("$.status").value("502"));
    }

    @Test
    @DisplayName("ConstraintViolationException returns 400 Bad Request")
    void handleConstraintViolation_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/constraint-violation").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.details").value("page: must be greater than or equal to 1"));
    }

    @Test
    @DisplayName("Generic Exception returns 500 Internal Server Error")
    void handleGenericException_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/generic-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }
}
