package com.imprint.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "imprint.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
}
