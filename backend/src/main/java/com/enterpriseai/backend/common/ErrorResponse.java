package com.enterpriseai.backend.common;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Standard error response envelope.")
public class ErrorResponse {

    @Schema(description = "Always false for error responses.", example = "false")
    private final boolean success = false;

    @Schema(description = "Human-readable error message.", example = "Invalid JWT token")
    private final String message;

    @Schema(description = "Time when the error response was generated.", example = "2026-07-13T19:45:00")
    private final LocalDateTime timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
