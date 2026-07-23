package com.enterpriseai.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "document.storage")
public record DocumentStorageProperties(String location) {
}
