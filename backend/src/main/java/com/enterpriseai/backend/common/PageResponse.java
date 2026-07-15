package com.enterpriseai.backend.common;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Reusable paginated response metadata and content.")
public class PageResponse<T> {

    @Schema(description = "Items on the current page.")
    private final List<T> content;

    @Schema(description = "Zero-based page number.", example = "0")
    private final int page;

    @Schema(description = "Requested page size.", example = "10")
    private final int size;

    @Schema(description = "Total number of matching items.", example = "42")
    private final long totalElements;

    @Schema(description = "Total number of pages.", example = "5")
    private final int totalPages;

    @Schema(description = "Whether this is the first page.", example = "true")
    private final boolean first;

    @Schema(description = "Whether this is the last page.", example = "false")
    private final boolean last;

    private PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page);
    }
}
