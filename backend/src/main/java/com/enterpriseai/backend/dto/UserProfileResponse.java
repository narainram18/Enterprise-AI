package com.enterpriseai.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Authenticated user's profile details.")
public class UserProfileResponse {

    @Schema(description = "User identifier.", example = "1")
    private Long id;

    @Schema(description = "Full name of the user.", example = "Naraian Kumar")
    private String name;

    @Schema(description = "Email address of the user.", example = "naraian@example.com")
    private String email;

    @Schema(description = "Application role assigned to the user.", example = "USER")
    private String role;
}
