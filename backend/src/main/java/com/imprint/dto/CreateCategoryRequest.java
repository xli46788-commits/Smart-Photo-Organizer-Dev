package com.imprint.dto;

import java.util.List;

public class CreateCategoryRequest {

    private String name;
    private List<String> keywords;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
