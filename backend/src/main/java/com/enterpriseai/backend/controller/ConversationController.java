package com.enterpriseai.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.config.OpenApiConfig;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.ConversationResponse;
import com.enterpriseai.backend.dto.ConversationSummaryResponse;
import com.enterpriseai.backend.dto.CreateConversationRequest;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.dto.RenameConversationRequest;
import com.enterpriseai.backend.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Endpoints for managing conversations and chat messages")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @Operation(
            summary = "Create conversation",
            description = "Creates a new conversation for the authenticated user.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ConversationSummaryResponse response = conversationService.createConversation(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Conversation created successfully", response));
    }

    @GetMapping
    @Operation(
            summary = "List conversations",
            description = "Returns a paginated list of conversations owned by the authenticated user.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> getConversations(
            Pageable pageable,
            @Parameter(hidden = true) Authentication authentication) {
        Page<ConversationSummaryResponse> page = conversationService.getConversations(authentication.getName(), pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversations fetched successfully", PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get conversation",
            description = "Returns the owned conversation including its messages.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        ConversationResponse response = conversationService.getConversation(id, authentication.getName());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversation fetched successfully", response));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Rename conversation",
            description = "Renames an owned conversation.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> renameConversation(
            @PathVariable Long id,
            @Valid @RequestBody RenameConversationRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ConversationSummaryResponse response = conversationService.renameConversation(id, authentication.getName(), request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversation renamed successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete conversation",
            description = "Deletes an owned conversation and all its messages.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        conversationService.deleteConversation(id, authentication.getName());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversation deleted successfully", null));
    }

    @PostMapping("/{id}/messages")
    @Operation(
            summary = "Add message",
            description = "Adds a user message to the specified conversation.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ChatMessageResponse>> addUserMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateMessageRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ChatMessageResponse response = conversationService.addUserMessage(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Message added successfully", response));
    }

    @GetMapping("/{id}/messages")
    @Operation(
            summary = "Get messages",
            description = "Returns all messages in the specified owned conversation.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        List<ChatMessageResponse> response = conversationService.getMessages(id, authentication.getName());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Messages fetched successfully", response));
    }
}
