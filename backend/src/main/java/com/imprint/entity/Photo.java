package com.imprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("photo")
public class Photo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long albumId;
    private String filePath;
    private String thumbPath;
    private String originalName;
    private String aiCategory;
    private BigDecimal aiConfidence;
    private LocalDateTime takenAt;
    private LocalDateTime createdAt;
}
