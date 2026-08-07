package com.enterpriseai.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.config.OpenApiConfig;
import com.enterpriseai.backend.ai.service.AiChatService;
import com.enterpriseai.backend.ai.service.AiStreamingChatService;
import com.enterpriseai.backend.dto.AiChatTurnResponse;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.ConversationResponse;
import com.enterpriseai.backend.dto.ConversationSummaryResponse;
import com.enterpriseai.backend.dto.CreateConversationRequest;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.dto.UpdateConversationRequest;
import com.enterpriseai.backend.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Endpoints for managing conversations and chat messages")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationService conversationService;
    private final AiChatService aiChatService;
    private final AiStreamingChatService aiStreamingChatService;

    public ConversationController(
            ConversationService conversationService,
            AiChatService aiChatService,
            AiStreamingChatService aiStreamingChatService) {
        this.conversationService = conversationService;
        this.aiChatService = aiChatService;
        this.aiStreamingChatService = aiStreamingChatService;
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
            summary = "Update conversation",
            description = "Updates an owned conversation (title, pin, favorite, archive).",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> updateConversation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConversationRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ConversationSummaryResponse response = conversationService.updateConversation(id, authentication.getName(), request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversation updated successfully", response));
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
            summary = "Send chat message",
            description = "Adds a user message and generates an assistant response in the specified conversation.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<AiChatTurnResponse>> addUserMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateMessageRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        AiChatTurnResponse response = aiChatService.chat(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Chat response generated successfully", response));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/messages/{messageId}")
    @Operation(
            summary = "Edit chat message",
            description = "Edits a user message.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public ResponseEntity<ApiResponse<ChatMessageResponse>> editMessage(
            @PathVariable Long id,
            @PathVariable Long messageId,
            @Valid @RequestBody CreateMessageRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ChatMessageResponse response = conversationService.editMessage(id, messageId, authentication.getName(), request.getContent());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Message updated successfully", response));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream chat message",
            description = "Adds a user message and streams the assistant response using server-sent events.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public SseEmitter streamMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateMessageRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        log.info("SSE request received conversationId={} user={}", id, authentication.getName());
        return aiStreamingChatService.stream(id, authentication.getName(), request);
    }

    @PostMapping(value = "/{id}/messages/{messageId}/regenerate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Regenerate chat response",
            description = "Regenerates the assistant response for an existing owned user message without duplicating that message.",
            security = @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME))
    public SseEmitter regenerateMessage(
            @PathVariable Long id,
            @PathVariable Long messageId,
            @Parameter(hidden = true) Authentication authentication) {
        return aiStreamingChatService.regenerate(id, messageId, authentication.getName());
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
