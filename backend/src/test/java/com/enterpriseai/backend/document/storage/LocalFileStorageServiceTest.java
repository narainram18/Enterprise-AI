package com.enterpriseai.backend.document.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.enterpriseai.backend.document.config.DocumentStorageProperties;

class LocalFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    private Path storageRoot;
    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageRoot = temporaryDirectory.resolve("documents");
        storageService = new LocalFileStorageService(
                new DocumentStorageProperties(storageRoot.toString()));
    }

    @Test
    void storesFileAndCreatesStorageDirectory() throws IOException {
        String storageKey = storageService.store(file("report.txt", "document content"));

        assertTrue(Files.isDirectory(storageRoot));
        assertTrue(Files.isRegularFile(storageRoot.resolve(storageKey)));
        assertEquals("document content", Files.readString(storageRoot.resolve(storageKey)));
    }

    @Test
    void preservesUploadedContentExactly() throws IOException {
        byte[] content = new byte[]{0, 1, 2, 3, 127, -1};
        String storageKey = storageService.store(
                new MockMultipartFile("file", "payload.bin", "application/octet-stream", content));

        assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(storageKey)));
    }

    @Test
    void generatesUniqueTrustedStorageKeys() {
        String firstKey = storageService.store(file("first.txt", "one"));
        String secondKey = storageService.store(file("second.txt", "two"));

        assertNotEquals(firstKey, secondKey);
        assertDoesNotThrow(() -> UUID.fromString(firstKey));
        assertDoesNotThrow(() -> UUID.fromString(secondKey));
    }

    @Test
    void maliciousOriginalFilenameCannotEscapeStorageRoot() throws IOException {
        String storageKey = storageService.store(
                file("../../secret.txt", "safe content"));

        List<Path> storedFiles;
        try (var files = Files.list(storageRoot)) {
            storedFiles = files.toList();
        }

        assertEquals(1, storedFiles.size());
        assertEquals(storageRoot.resolve(storageKey), storedFiles.get(0));
        assertFalse(Files.exists(temporaryDirectory.resolve("secret.txt")));
        assertThrows(FileStorageException.class,
                () -> storageService.delete("../../secret.txt"));
    }

    @Test
    void deletesStoredFile() throws IOException {
        String storageKey = storageService.store(file("delete.txt", "delete me"));
        Path storedFile = storageRoot.resolve(storageKey);

        storageService.delete(storageKey);

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void missingFileDeletionIsSafe() {
        assertDoesNotThrow(() -> storageService.delete(UUID.randomUUID().toString()));
    }

    private MockMultipartFile file(String originalFilename, String content) {
        return new MockMultipartFile(
                "file",
                originalFilename,
                "text/plain",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
