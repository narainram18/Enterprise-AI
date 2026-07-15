package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;

import com.enterpriseai.backend.entity.MessageRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private MessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
