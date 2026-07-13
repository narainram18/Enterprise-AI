package com.enterpriseai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.dto.AuthResponse;
import com.enterpriseai.backend.dto.LogoutRequest;
import com.enterpriseai.backend.dto.RefreshTokenRequest;
import com.enterpriseai.backend.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, refresh, and logout APIs.")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Rotates a valid refresh token and returns a new JWT access token plus a new refresh token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Token refreshed successfully",
                                      "data": {
                                        "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYXJhaWFuQGV4YW1wbGUuY29tIn0.signature",
                                        "refreshToken": "kF3pX9QrKXy0bXlOQYCX0qF7wDsC7zm9pYqtX-kRx5WcseUrXaxMkwtxTgK6BbMQolrF3StVmS8SlMge8hUFsg"
                                      },
                                      "timestamp": "2026-07-13T20:10:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Missing refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid, expired, or revoked refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Validated @RequestBody RefreshTokenRequest request) {

        AuthResponse response =
                refreshTokenService.rotate(request.getRefreshToken());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Token refreshed successfully",
                        response
                )
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Revokes the provided refresh token so it cannot be used again.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Logout successful",
                                      "data": null,
                                      "timestamp": "2026-07-13T20:10:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Missing refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.enterpriseai.backend.common.ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Validated @RequestBody LogoutRequest request) {

        refreshTokenService.revoke(request.getRefreshToken());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Logout successful",
                        null
                )
        );
    }
}
