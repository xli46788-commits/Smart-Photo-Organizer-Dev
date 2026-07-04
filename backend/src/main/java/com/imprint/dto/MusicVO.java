package com.imprint.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MusicVO {

    private Long id;
    private String originalName;
    private String url;
    private Long fileSize;
    private LocalDateTime createdAt;
}
