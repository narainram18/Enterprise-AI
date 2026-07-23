package com.enterpriseai.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document.upload")
public record DocumentUploadProperties(long maxSize) {
}
