package com.enterpriseai.backend.document.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "document.chunking")
public class DocumentChunkingProperties {
    
    private int chunkSize = 1500;
    
    private int chunkOverlap = 200;
}
