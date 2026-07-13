package com.enterpriseai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload used to authenticate an existing user.")
public class LoginRequest {

    @Schema(
            description = "Registered email address.",
            example = "naraian@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(
            description = "Account password.",
            example = "StrongPass123",
            accessMode = Schema.AccessMode.WRITE_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;
}
