package com.enterpriseai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload used to rotate a refresh token and issue a new access token.")
public class RefreshTokenRequest {

    @Schema(
            description = "Active refresh token issued during login or previous refresh.",
            example = "kF3pX9QrKXy0bXlOQYCX0qF7wDsC7zm9pYqtX-kRx5WcseUrXaxMkwtxTgK6BbMQolrF3StVmS8SlMge8hUFsg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
