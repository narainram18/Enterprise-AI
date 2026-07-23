package com.enterpriseai.backend.document.extractor;

import java.io.InputStream;

import com.enterpriseai.backend.entity.DocumentType;

public interface TextExtractor {

    boolean supports(DocumentType documentType);

    String extract(InputStream inputStream);
}
