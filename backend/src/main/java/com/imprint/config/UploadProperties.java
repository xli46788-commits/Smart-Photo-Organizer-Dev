package com.imprint.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "imprint.upload")
public class UploadProperties {

    private String dir = "uploads";

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(dir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        Files.createDirectories(path);
        this.dir = path.toString();
    }
}
