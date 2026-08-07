package com.enterpriseai.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConversationRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private Boolean pinned;
    private Boolean favorite;
    private Boolean archived;
}
