package com.enterpriseai.backend.dto;

import java.time.LocalDateTime;

public record Activity(
    String id,
    String type, // CONVERSATION or DOCUMENT
    String title,
    String description,
    LocalDateTime timestamp,
    String url
) {}
