package com.enterpriseai.backend.ai.tool;

public record ToolParameter(
        String name,
        String type,
        String description,
        boolean required
) {
}
