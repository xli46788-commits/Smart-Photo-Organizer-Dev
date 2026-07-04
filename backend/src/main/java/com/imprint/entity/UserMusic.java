package com.imprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_music")
public class UserMusic {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String filePath;
    private String originalName;
    private Long fileSize;
    private LocalDateTime createdAt;
}
