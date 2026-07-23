package com.enterpriseai.backend.document.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.enterpriseai.backend.entity.DocumentType;

@Component
public class DocxTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentType documentType) {
        return DocumentType.DOCX == documentType;
    }

    @Override
    public String extract(InputStream inputStream) {
        if (inputStream == null) {
            throw new TextExtractionException("DOCX input is required");
        }

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (IBodyElement bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph paragraph) {
                    appendLine(text, paragraph.getText());
                } else if (bodyElement instanceof XWPFTable table) {
                    appendTable(text, table);
                }
            }

            if (text.toString().isBlank()) {
                throw new TextExtractionException("DOCX contains no extractable text");
            }
            return text.toString().strip();
        } catch (TextExtractionException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new TextExtractionException("Could not extract text from DOCX", ex);
        }
    }

    private void appendTable(StringBuilder text, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            String rowText = row.getTableCells().stream()
                    .map(XWPFTableCell::getText)
                    .collect(Collectors.joining("\t"));
            appendLine(text, rowText);
        }
    }

    private void appendLine(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            text.append(value.strip()).append('\n');
        }
    }
}
