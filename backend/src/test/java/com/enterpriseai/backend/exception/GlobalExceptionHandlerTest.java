package com.enterpriseai.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.enterpriseai.backend.common.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOversizedMultipartUploadsToPayloadTooLarge() {
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(
                mock(MaxUploadSizeExceededException.class));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("Uploaded file exceeds the maximum allowed size",
                response.getBody().getMessage());
    }
}
