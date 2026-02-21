package com.cinesearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Frontend request body for POST /api/ai/parse
 */
public class AiParseRequest {

    @NotBlank(message = "User text is required")
    @Size(min = 2, max = 1000, message = "Text must be between 2 and 1000 characters")
    private String text;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
