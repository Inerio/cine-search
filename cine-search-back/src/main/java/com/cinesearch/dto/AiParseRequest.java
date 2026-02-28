package com.cinesearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiParseRequest(
    @NotBlank(message = "User text is required")
    @Size(min = 2, max = 1000, message = "Text must be between 2 and 1000 characters")
    String text,
    String mediaType
) {}
