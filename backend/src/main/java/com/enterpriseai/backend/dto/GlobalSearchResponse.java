package com.enterpriseai.backend.dto;

import java.util.List;
import java.util.Map;

public record GlobalSearchResponse(
    String query,
    Map<String, List<SearchResultItem>> resultsByCategory,
    int totalResults
) {}
