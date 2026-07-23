package com.enterpriseai.backend.document.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.enterpriseai.backend.entity.DocumentType;

@Component
public class PlainTextExtractor implements TextExtractor {

    @Override
    public boolean supports(DocumentType documentType) {
        return DocumentType.TXT == documentType;
    }

    @Override
    public String extract(InputStream inputStream) {
        if (inputStream == null) {
            throw new TextExtractionException("TXT input is required");
        }

        try {
            byte[] bytes = inputStream.readAllBytes();
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            String text = decoded.toString();
            if (text.isBlank()) {
                throw new TextExtractionException("TXT contains no extractable text");
            }
            return text.strip();
        } catch (CharacterCodingException ex) {
            throw new TextExtractionException("TXT content is not valid UTF-8", ex);
        } catch (IOException ex) {
            throw new TextExtractionException("Could not read TXT content", ex);
        }
    }
}
