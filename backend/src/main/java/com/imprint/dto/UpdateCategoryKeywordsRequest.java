package com.imprint.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateCategoryKeywordsRequest {

    private List<String> keywords;
}
