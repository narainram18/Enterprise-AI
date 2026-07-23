package com.enterpriseai.backend.document.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.enterpriseai.backend.document.config.DocumentStorageProperties;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String STORAGE_KEY_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private final Path storageRoot;

    public LocalFileStorageService(DocumentStorageProperties properties) {
        if (properties == null || properties.location() == null || properties.location().isBlank()) {
            throw new FileStorageException("Document storage location is not configured");
        }
        this.storageRoot = Paths.get(properties.location()).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null) {
            throw new FileStorageException("Cannot store a null file");
        }

        String storageKey = UUID.randomUUID().toString();
        Path target = resolveTrustedPath(storageKey);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target);
            return storageKey;
        } catch (IOException ex) {
            deleteQuietly(target);
            throw new FileStorageException("Could not store file", ex);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        Path target = resolveTrustedPath(storageKey);
        try {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new FileStorageException("Stored file is not available");
            }
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw new FileStorageException("Could not open stored file", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveTrustedPath(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file", ex);
        }
    }

    private Path resolveTrustedPath(String storageKey) {
        ensureStorageRoot();

        if (storageKey == null || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            throw new FileStorageException("Invalid storage key");
        }

        Path candidate = storageRoot.resolve(storageKey).normalize();
        if (!candidate.startsWith(storageRoot) || !candidate.getParent().equals(storageRoot)) {
            throw new FileStorageException("Invalid storage key");
        }

        try {
            Path realRoot = storageRoot.toRealPath();
            Path realParent = candidate.getParent().toRealPath();
            if (!realParent.startsWith(realRoot)) {
                throw new FileStorageException("Invalid storage path");
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not verify storage path", ex);
        }

        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                && Files.isSymbolicLink(candidate)) {
            throw new FileStorageException("Invalid storage path");
        }

        return candidate;
    }

    private void ensureStorageRoot() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize file storage", ex);
        }
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Preserve the original storage failure without exposing filesystem details.
        }
    }
}
