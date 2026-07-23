package com.enterpriseai.backend.document.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import com.enterpriseai.backend.entity.DocumentType;

class TextExtractionTest {

    private final PlainTextExtractor plainTextExtractor = new PlainTextExtractor();
    private final PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();
    private final DocxTextExtractor docxTextExtractor = new DocxTextExtractor();

    @Test
    void extractsUtf8PlainText() {
        String text = "Hello, extraction!\nRésumé and 日本語";

        String extracted = plainTextExtractor.extract(input(text.getBytes(StandardCharsets.UTF_8)));

        assertEquals(text, extracted);
    }

    @Test
    void rejectsInvalidUtf8PlainText() {
        assertThrows(TextExtractionException.class,
                () -> plainTextExtractor.extract(input(new byte[]{(byte) 0xC3, 0x28})));
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        byte[] pdf = createPdf("PDF text extraction works");

        String extracted = pdfTextExtractor.extract(input(pdf));

        assertTrue(extracted.contains("PDF text extraction works"));
    }

    @Test
    void extractsParagraphsAndTableCellsFromDocx() throws IOException {
        byte[] docx;
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("First paragraph");
            document.createParagraph().createRun().setText("Second paragraph");
            var table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Header A");
            table.getRow(0).getCell(1).setText("Header B");
            table.getRow(1).getCell(0).setText("Cell A");
            table.getRow(1).getCell(1).setText("Cell B");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.write(output);
            docx = output.toByteArray();
        }

        String extracted = docxTextExtractor.extract(input(docx));

        assertTrue(extracted.contains("First paragraph"));
        assertTrue(extracted.contains("Second paragraph"));
        assertTrue(extracted.contains("Header A\tHeader B"));
        assertTrue(extracted.contains("Cell A\tCell B"));
    }

    @Test
    void rejectsPdfWithNoMeaningfulText() throws IOException {
        byte[] emptyPdf;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            emptyPdf = output.toByteArray();
        }

        assertThrows(TextExtractionException.class,
                () -> pdfTextExtractor.extract(input(emptyPdf)));
    }

    @Test
    void rejectsEmptyPlainText() {
        assertThrows(TextExtractionException.class,
                () -> plainTextExtractor.extract(input(new byte[0])));
    }

    @Test
    void dispatcherRejectsUnsupportedTypeWhenNoExtractorSupportsIt() {
        TextExtractionService dispatcher = new TextExtractionService(
                List.of(plainTextExtractor));

        assertThrows(TextExtractionException.class,
                () -> dispatcher.extract(DocumentType.PDF, input("not a pdf".getBytes(StandardCharsets.UTF_8))));
    }

    private ByteArrayInputStream input(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
