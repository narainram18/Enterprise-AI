package com.enterpriseai.backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.ConversationResponse;
import com.enterpriseai.backend.dto.ConversationSummaryResponse;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.entity.Conversation;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConversationMapper {

    @Mapping(target = "citations", ignore = true)
    @Mapping(target = "retrievalStatistics", ignore = true)
    ChatMessageResponse toMessageResponse(ChatMessage message);

    List<ChatMessageResponse> toMessageResponseList(List<ChatMessage> messages);

    ConversationSummaryResponse toSummaryResponse(Conversation conversation);

    @Mapping(target = "messages", source = "messages")
    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "title", source = "conversation.title")
    @Mapping(target = "createdAt", source = "conversation.createdAt")
    @Mapping(target = "updatedAt", source = "conversation.updatedAt")
    @Mapping(target = "agentId", source = "conversation.agentId")
    ConversationResponse toConversationResponse(Conversation conversation, List<ChatMessageResponse> messages);
}
