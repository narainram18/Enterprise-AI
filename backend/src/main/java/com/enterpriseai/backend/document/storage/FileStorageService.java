package com.enterpriseai.backend.document.storage;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file);

    InputStream open(String storageKey);

    void delete(String storageKey);
}
