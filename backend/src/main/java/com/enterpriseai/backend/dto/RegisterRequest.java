package com.enterpriseai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload used to create a new user account.")
public class RegisterRequest {

    @Schema(
            description = "Full name of the user.",
            example = "Naraian Kumar",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(
            description = "Unique email address used for login.",
            example = "naraian@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(
            description = "Plain text password. It is hashed before storage.",
            example = "StrongPass123",
            minLength = 6,
            accessMode = Schema.AccessMode.WRITE_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
