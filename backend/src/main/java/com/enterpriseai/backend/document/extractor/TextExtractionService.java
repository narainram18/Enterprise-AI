package com.enterpriseai.backend.document.extractor;

import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.enterpriseai.backend.entity.DocumentType;

@Service
public class TextExtractionService {

    private final List<TextExtractor> extractors;

    public TextExtractionService(List<TextExtractor> extractors) {
        this.extractors = List.copyOf(extractors);
    }

    public String extract(DocumentType documentType, InputStream inputStream) {
        TextExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(documentType))
                .findFirst()
                .orElseThrow(() -> new TextExtractionException(
                        "Unsupported document type: " + documentType));
        return extractor.extract(inputStream);
    }
}
