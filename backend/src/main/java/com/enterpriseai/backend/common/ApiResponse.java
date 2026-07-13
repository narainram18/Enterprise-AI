package com.enterpriseai.backend.common;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Standard successful API response envelope.")
public class ApiResponse<T> {

    @Schema(description = "Indicates whether the request succeeded.", example = "true")
    private final boolean success;

    @Schema(description = "Human-readable response message.", example = "Request completed successfully")
    private final String message;

    @Schema(description = "Response payload. The type depends on the endpoint.")
    private final T data;

    @Schema(description = "Time when the response was generated.", example = "2026-07-13T19:45:00")
    private final LocalDateTime timestamp;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
