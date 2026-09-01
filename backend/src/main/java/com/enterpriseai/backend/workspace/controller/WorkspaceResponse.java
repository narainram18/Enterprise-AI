package com.enterpriseai.backend.workspace.controller;

import java.time.LocalDateTime;

public record WorkspaceResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean onlineMode
) {}
