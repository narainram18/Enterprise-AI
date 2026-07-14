package com.enterpriseai.backend.common;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response envelope.")
public class ErrorResponse {

    @Schema(description = "Always false for error responses.", example = "false")
    private final boolean success = false;

    @Schema(description = "Human-readable error message.", example = "Invalid JWT token")
    private final String message;

    @Schema(description = "Field-wise validation errors.")
    private final Map<String, String> errors;

    @Schema(description = "Time when the error response was generated.", example = "2026-07-13T19:45:00")
    private final LocalDateTime timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.errors = null;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(Map<String, String> errors) {
        this.message = null;
        this.errors = errors;
        this.timestamp = null;
    }
}
