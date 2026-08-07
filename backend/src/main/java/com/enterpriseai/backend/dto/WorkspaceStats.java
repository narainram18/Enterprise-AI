package com.enterpriseai.backend.dto;

public record WorkspaceStats(
    long conversationsCount,
    long documentsCount,
    long agentsCount,
    long membersCount,
    long storageUsedBytes
) {}
