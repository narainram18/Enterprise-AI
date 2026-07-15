package com.enterpriseai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.config.OpenApiConfig;
import com.enterpriseai.backend.dto.UserProfileResponse;
import com.enterpriseai.backend.service.AuthService;
import com.enterpriseai.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Users", description = "Authenticated user APIs.")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Get current user profile",
            description = "Returns profile information for the user represented by the JWT.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "User profile fetched successfully",
                                      "data": {
                                        "id": 1,
                                        "name": "Naraian Kumar",
                                        "email": "naraian@example.com",
                                        "role": "USER"
                                      },
                                      "timestamp": "2026-07-13T19:45:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing, invalid, or expired JWT",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user no longer exists",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @Parameter(hidden = true)
            Authentication authentication) {

        UserProfileResponse user =
                authService.getCurrentUser(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User profile fetched successfully",
                        user
                )
        );
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List users",
            description = "Returns a paginated, searchable, and filterable user list for administrators.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {

        PageResponse<UserProfileResponse> users = userService.searchUsers(
                page, size, sortBy, direction, search, role);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users fetched successfully", users));
    }
}
