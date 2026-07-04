package com.imprint.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PhotoVO {

    private Long id;
    private String originalName;
    private String url;
    private String thumbUrl;
    private String aiCategory;
    private BigDecimal aiConfidence;
    private LocalDateTime takenAt;
    private LocalDateTime createdAt;
}
