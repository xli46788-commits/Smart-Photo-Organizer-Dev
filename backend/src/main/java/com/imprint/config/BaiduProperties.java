package com.imprint.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "imprint.baidu")
public class BaiduProperties {

    private String apiKey;
    private String secretKey;
    private boolean enabled = false;
}
