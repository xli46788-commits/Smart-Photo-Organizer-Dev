package com.imprint.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {

    private Long id;
    private String name;
    private Boolean builtIn;
    private Long photoCount;
    private List<String> keywords;
}
