package com.enterpriseai.backend.dto;

public record SearchResultItem(
    String id,
    String type,
    String title,
    String description,
    String url,
    String timestamp,
    String matchReason
) {}
