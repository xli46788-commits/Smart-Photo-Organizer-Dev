package com.imprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imprint.config.BaiduProperties;
import com.imprint.service.CategoryMapper.ClassifyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

@Service
public class BaiduAiService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAiService.class);
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String CLASSIFY_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general";

    private final BaiduProperties baiduProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String cachedToken;
    private long tokenExpireAt;

    public BaiduAiService(BaiduProperties baiduProperties, ObjectMapper objectMapper) {
        this.baiduProperties = baiduProperties;
        this.objectMapper = objectMapper;
    }

    public ClassifyResult classify(Path imagePath) {
        if (!baiduProperties.isEnabled()) {
            log.warn("百度 AI 未启用，使用文件名兜底分类");
            return classifyByFilename(imagePath);
        }
        try {
            String token = getAccessToken();
            byte[] bytes = Files.readAllBytes(imagePath);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String body = "image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLASSIFY_URL + "?access_token=" + token))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("error_code")) {
                log.error("百度 AI 返回错误: {}", response.body());
                return classifyByFilename(imagePath);
            }

            JsonNode result = root.path("result");
            if (result.isArray() && !result.isEmpty()) {
                for (JsonNode item : result) {
                    String keyword = item.path("keyword").asText("");
                    BigDecimal score = BigDecimal.valueOf(item.path("score").asDouble(0))
                            .setScale(4, RoundingMode.HALF_UP);
                    ClassifyResult mapped = CategoryMapper.mapKeyword(keyword, score);
                    if (!CategoryMapper.OTHER.equals(mapped.getCategory())) {
                        log.info("百度 AI 识别: {} -> {} ({})", keyword, mapped.getCategory(), mapped.getConfidence());
                        return mapped;
                    }
                }
                JsonNode top = result.get(0);
                String keyword = top.path("keyword").asText("");
                BigDecimal score = BigDecimal.valueOf(top.path("score").asDouble(0))
                        .setScale(4, RoundingMode.HALF_UP);
                ClassifyResult mapped = CategoryMapper.mapKeyword(keyword, score);
                log.info("百度 AI 识别: {} -> {} ({})", keyword, mapped.getCategory(), mapped.getConfidence());
                return mapped;
            }
            log.warn("百度 AI 无识别结果: {}", response.body());
        } catch (Exception e) {
            log.error("百度 AI 调用失败，使用文件名兜底", e);
            return classifyByFilename(imagePath);
        }
        return new ClassifyResult(CategoryMapper.OTHER, BigDecimal.ZERO, "");
    }

    private ClassifyResult classifyByFilename(Path imagePath) {
        String name = imagePath.getFileName().toString().toLowerCase();
        return CategoryMapper.mapKeyword(name, BigDecimal.valueOf(0.5));
    }

    private String getAccessToken() throws IOException, InterruptedException {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return cachedToken;
        }
        String url = TOKEN_URL + "?grant_type=client_credentials"
                + "&client_id=" + baiduProperties.getApiKey()
                + "&client_secret=" + baiduProperties.getSecretKey();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        cachedToken = root.path("access_token").asText();
        int expiresIn = root.path("expires_in").asInt(2592000);
        tokenExpireAt = System.currentTimeMillis() + (expiresIn - 300L) * 1000L;
        return cachedToken;
    }
}
