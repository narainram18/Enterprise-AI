package com.enterpriseai.backend.health;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DocumentStorageHealthIndicator implements HealthIndicator {

    private final Path storageLocation;

    public DocumentStorageHealthIndicator(@Value("${document.storage.location}") String location) {
        this.storageLocation = Paths.get(location);
    }

    @Override
    public Health health() {
        try {
            if (!Files.exists(storageLocation)) {
                Files.createDirectories(storageLocation);
            }

            if (!Files.isWritable(storageLocation)) {
                return Health.down().withDetail("service", "DocumentStorage").withDetail("error", "Directory is not writable").build();
            }

            long freeSpace = Files.getFileStore(storageLocation).getUsableSpace();
            long totalSpace = Files.getFileStore(storageLocation).getTotalSpace();

            return Health.up()
                    .withDetail("service", "DocumentStorage")
                    .withDetail("freeSpaceBytes", freeSpace)
                    .withDetail("totalSpaceBytes", totalSpace)
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("service", "DocumentStorage").build();
        }
    }
}
