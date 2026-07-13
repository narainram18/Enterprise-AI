package com.enterpriseai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Authentication response returned after successful login or token refresh.")
public class AuthResponse {

    @Schema(
            description = "JWT access token used in the Authorization header.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYXJhaWFuQGV4YW1wbGUuY29tIn0.signature")
    private String token;

    @Schema(
            description = "Refresh token used to obtain a new JWT access token.",
            example = "kF3pX9QrKXy0bXlOQYCX0qF7wDsC7zm9pYqtX-kRx5WcseUrXaxMkwtxTgK6BbMQolrF3StVmS8SlMge8hUFsg")
    private String refreshToken;

    public AuthResponse(String token) {
        this(token, null);
    }

    public AuthResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }
}
