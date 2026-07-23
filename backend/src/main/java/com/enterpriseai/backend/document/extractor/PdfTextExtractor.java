package com.enterpriseai.backend.document.extractor;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.entity.DocumentType;

@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentType documentType) {
        return DocumentType.PDF == documentType;
    }

    @Override
    public String extract(InputStream inputStream) {
        if (inputStream == null) {
            throw new TextExtractionException("PDF input is required");
        }

        try (PDDocument document = PDDocument.load(inputStream)) {
            String text = new PDFTextStripper().getText(document);
            return requireMeaningfulText(text, "PDF contains no extractable text");
        } catch (IOException ex) {
            throw new TextExtractionException("Could not extract text from PDF", ex);
        }
    }

    private String requireMeaningfulText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new TextExtractionException(message);
        }
        return text.strip();
    }
}
