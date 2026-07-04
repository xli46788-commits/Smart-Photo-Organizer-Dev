package com.imprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("album")
public class Album {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String category;
    private String coverUrl;
    private LocalDateTime createdAt;
}
