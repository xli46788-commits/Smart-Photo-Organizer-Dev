package com.imprint.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CategoryMapper {

    public static final String OTHER = "其他";

    private static final Map<String, String> KEYWORD_MAP = Map.ofEntries(
            Map.entry("食物", "食物"), Map.entry("菜", "食物"), Map.entry("餐", "食物"),
            Map.entry("水果", "食物"), Map.entry("蛋糕", "食物"), Map.entry("咖啡", "食物"),
            Map.entry("风景", "风景"), Map.entry("建筑", "风景"), Map.entry("天空", "风景"),
            Map.entry("山", "风景"), Map.entry("海", "风景"), Map.entry("树", "风景"),
            Map.entry("节日", "节日"), Map.entry("春节", "节日"), Map.entry("元宵", "节日"),
            Map.entry("七夕", "节日"), Map.entry("海报", "节日"),
            Map.entry("设计", "设计"), Map.entry("艺术", "设计"), Map.entry("logo", "设计"),
            Map.entry("美女", "人像"), Map.entry("人像", "人像"), Map.entry("自拍", "人像"),
            Map.entry("人物", "人像"), Map.entry("女孩", "人像"),
            Map.entry("动漫", "动漫"), Map.entry("卡通", "动漫"), Map.entry("漫画", "动漫"),
            Map.entry("猫", "动物"), Map.entry("狗", "动物"), Map.entry("宠物", "动物"),
            Map.entry("动物", "动物"), Map.entry("哺乳动物", "动物"), Map.entry("犬", "动物"),
            Map.entry("猫科", "动物"), Map.entry("鸟类", "动物"), Map.entry("鸟", "动物"),
            Map.entry("鱼", "动物"), Map.entry("兔子", "动物"), Map.entry("兔", "动物"),
            Map.entry("仓鼠", "动物"), Map.entry("家禽", "动物"), Map.entry("牲畜", "动物"),
            Map.entry("英短", "动物"), Map.entry("橘猫", "动物"), Map.entry("加菲", "动物")
    );

    private static final List<String> VALID_CATEGORIES = List.of(
            "节日", "风景", "设计", "人像", "动漫", "食物", "动物", OTHER
    );

    private static final Map<String, String> LEGACY_CATEGORY_MAP = Map.of(
            "FESTIVAL", "节日",
            "SCENE", "风景",
            "DESIGN", "设计",
            "BEAUTY", "人像",
            "ANIME", "动漫",
            "FOOD", "食物",
            "ANIMAL", "动物",
            "OTHER", OTHER
    );

    private CategoryMapper() {
    }

    public static ClassifyResult mapKeyword(String keyword, BigDecimal score) {
        if (keyword == null || keyword.isBlank()) {
            return new ClassifyResult(OTHER, score != null ? score : BigDecimal.ZERO, keyword);
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            if (keyword.contains(entry.getKey()) || lower.contains(entry.getKey())) {
                return new ClassifyResult(entry.getValue(), score, keyword);
            }
        }
        return new ClassifyResult(OTHER, score, keyword);
    }

    public static List<String> defaultKeywordsFor(String category) {
        return KEYWORD_MAP.entrySet().stream()
                .filter(e -> e.getValue().equals(category))
                .map(Map.Entry::getKey)
                .distinct()
                .toList();
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return OTHER;
        }
        if (VALID_CATEGORIES.contains(category)) {
            return category;
        }
        return LEGACY_CATEGORY_MAP.getOrDefault(category, category);
    }

    public static void validateCategory(String category) {
        String normalized = normalizeCategory(category);
        if (!VALID_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("无效分类: " + category);
        }
    }

    public static List<String> allCategories() {
        return VALID_CATEGORIES;
    }

    @Data
    @AllArgsConstructor
    public static class ClassifyResult {
        private String category;
        private BigDecimal confidence;
        private String sourceKeyword;
    }
}
