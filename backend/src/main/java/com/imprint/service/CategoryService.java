package com.imprint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imprint.dto.CategoryVO;
import com.imprint.entity.Photo;
import com.imprint.entity.UserCategory;
import com.imprint.mapper.PhotoMapper;
import com.imprint.mapper.UserCategoryMapper;
import com.imprint.service.CategoryMapper.ClassifyResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CategoryService {

  public static final String OTHER = CategoryMapper.OTHER;

  private static final List<String> DEFAULT_CATEGORIES = List.of(
      "节日", "风景", "设计", "人像", "动漫", "食物", "动物"
  );

  private final UserCategoryMapper userCategoryMapper;
  private final PhotoMapper photoMapper;
  private final PhotoService photoService;
  private final ObjectMapper objectMapper;

  public CategoryService(UserCategoryMapper userCategoryMapper,
                         PhotoMapper photoMapper,
                         @Lazy PhotoService photoService,
                         ObjectMapper objectMapper) {
    this.userCategoryMapper = userCategoryMapper;
    this.photoMapper = photoMapper;
    this.photoService = photoService;
    this.objectMapper = objectMapper;
  }

  public List<CategoryVO> listCategories(Long userId) {
    ensureDefaultCategories(userId);
    backfillDefaultKeywords(userId);
    return userCategoryMapper.selectList(new LambdaQueryWrapper<UserCategory>()
            .eq(UserCategory::getUserId, userId)
            .orderByAsc(UserCategory::getBuiltIn)
            .orderByAsc(UserCategory::getId))
        .stream()
        .map(this::toVO)
        .toList();
  }

  public CategoryVO createCategory(Long userId, String name, List<String> keywords) {
    ensureDefaultCategories(userId);
    String normalized = normalizeName(name);
    if (OTHER.equals(normalized)) {
      throw new IllegalArgumentException("「其他」为系统保留分类，无法重复创建");
    }
    if ("全部".equals(normalized) || "ALL".equalsIgnoreCase(normalized)) {
      throw new IllegalArgumentException("不能使用保留名称");
    }

    Long count = userCategoryMapper.selectCount(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId)
        .eq(UserCategory::getName, normalized));
    if (count > 0) {
      throw new IllegalArgumentException("分类名称已存在");
    }

    UserCategory category = new UserCategory();
    category.setUserId(userId);
    category.setName(normalized);
    category.setKeywords(serializeKeywords(normalizeKeywords(keywords)));
    category.setBuiltIn(false);
    category.setCreatedAt(LocalDateTime.now());
    userCategoryMapper.insert(category);
    return toVO(category);
  }

  public CategoryVO updateCategoryKeywords(Long userId, Long categoryId, List<String> keywords) {
    UserCategory category = getOwnedCategory(userId, categoryId);
    category.setKeywords(serializeKeywords(normalizeKeywords(keywords)));
    userCategoryMapper.updateById(category);
    return toVO(category);
  }

  @Transactional
  public void deleteCategory(Long userId, Long categoryId, boolean deletePhotos) throws IOException {
    UserCategory category = getOwnedCategory(userId, categoryId);
    if (Boolean.TRUE.equals(category.getBuiltIn())) {
      throw new IllegalArgumentException("系统分类「其他」不可删除");
    }

    String categoryName = category.getName();
    List<Photo> photos = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
        .eq(Photo::getUserId, userId)
        .eq(Photo::getAiCategory, categoryName));

    if (deletePhotos) {
      for (Photo photo : photos) {
        photoService.deletePhoto(userId, photo.getId());
      }
    } else if (!photos.isEmpty()) {
      photoMapper.update(null, new LambdaUpdateWrapper<Photo>()
          .eq(Photo::getUserId, userId)
          .eq(Photo::getAiCategory, categoryName)
          .set(Photo::getAiCategory, OTHER));
    }

    userCategoryMapper.deleteById(categoryId);
  }

  public void validateUserCategory(Long userId, String category) {
    ensureDefaultCategories(userId);
    String normalized = normalizeName(category);
    Long count = userCategoryMapper.selectCount(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId)
        .eq(UserCategory::getName, normalized));
    if (count == 0) {
      throw new IllegalArgumentException("无效分类: " + category);
    }
  }

  public String resolveForClassification(Long userId, ClassifyResult result, String filename) {
    ensureDefaultCategories(userId);
    List<UserCategory> categories = userCategoryMapper.selectList(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId));

    List<String> texts = new ArrayList<>();
    if (result != null) {
      if (StringUtils.hasText(result.getSourceKeyword())) {
        texts.add(result.getSourceKeyword());
      }
      if (StringUtils.hasText(result.getCategory())) {
        texts.add(result.getCategory());
      }
    }
    if (StringUtils.hasText(filename)) {
      texts.add(filename);
    }
    String combined = String.join(" ", texts).toLowerCase(Locale.ROOT);

    String bestCategory = null;
    int bestLen = 0;

    for (UserCategory category : categories) {
      if (OTHER.equals(category.getName())) {
        continue;
      }
      Set<String> keywordSet = new LinkedHashSet<>(parseKeywords(category.getKeywords()));
      keywordSet.add(category.getName());
      for (String keyword : keywordSet) {
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.length() < 1) {
          continue;
        }
        if (combined.contains(normalizedKeyword) && normalizedKeyword.length() > bestLen) {
          bestLen = normalizedKeyword.length();
          bestCategory = category.getName();
        }
      }
    }

    if (bestCategory != null) {
      return bestCategory;
    }
    return resolveCategoryForUser(userId, result != null ? result.getCategory() : OTHER);
  }

  public String resolveCategoryForUser(Long userId, String aiCategory) {
    ensureDefaultCategories(userId);
    String normalized = CategoryMapper.normalizeCategory(aiCategory);
    Long count = userCategoryMapper.selectCount(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId)
        .eq(UserCategory::getName, normalized));
    return count > 0 ? normalized : OTHER;
  }

  public void ensureDefaultCategories(Long userId) {
    Long count = userCategoryMapper.selectCount(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId));
    if (count > 0) {
      return;
    }
    seedDefaultCategories(userId);
  }

  public void seedDefaultCategories(Long userId) {
    for (String name : DEFAULT_CATEGORIES) {
      UserCategory category = new UserCategory();
      category.setUserId(userId);
      category.setName(name);
      category.setKeywords(serializeKeywords(CategoryMapper.defaultKeywordsFor(name)));
      category.setBuiltIn(false);
      category.setCreatedAt(LocalDateTime.now());
      userCategoryMapper.insert(category);
    }
    UserCategory other = new UserCategory();
    other.setUserId(userId);
    other.setName(OTHER);
    other.setBuiltIn(true);
    other.setCreatedAt(LocalDateTime.now());
    userCategoryMapper.insert(other);
  }

  private void backfillDefaultKeywords(Long userId) {
    List<UserCategory> categories = userCategoryMapper.selectList(new LambdaQueryWrapper<UserCategory>()
        .eq(UserCategory::getUserId, userId));
    for (UserCategory category : categories) {
      if (StringUtils.hasText(category.getKeywords()) || !DEFAULT_CATEGORIES.contains(category.getName())) {
        continue;
      }
      category.setKeywords(serializeKeywords(CategoryMapper.defaultKeywordsFor(category.getName())));
      userCategoryMapper.updateById(category);
    }
  }

  private UserCategory getOwnedCategory(Long userId, Long categoryId) {
    UserCategory category = userCategoryMapper.selectById(categoryId);
    if (category == null || !category.getUserId().equals(userId)) {
      throw new IllegalArgumentException("分类不存在");
    }
    return category;
  }

  private CategoryVO toVO(UserCategory category) {
    CategoryVO vo = new CategoryVO();
    vo.setId(category.getId());
    vo.setName(category.getName());
    vo.setBuiltIn(Boolean.TRUE.equals(category.getBuiltIn()));
    vo.setKeywords(parseKeywords(category.getKeywords()));
    Long photoCount = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
        .eq(Photo::getUserId, category.getUserId())
        .eq(Photo::getAiCategory, category.getName()));
    vo.setPhotoCount(photoCount);
    return vo;
  }

  private List<String> normalizeKeywords(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return List.of();
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String keyword : keywords) {
      if (!StringUtils.hasText(keyword)) {
        continue;
      }
      String trimmed = keyword.trim();
      if (trimmed.length() > 20) {
        throw new IllegalArgumentException("关键词不能超过 20 个字符: " + trimmed);
      }
      normalized.add(trimmed);
      if (normalized.size() > 30) {
        throw new IllegalArgumentException("每个分类最多 30 个关键词");
      }
    }
    return new ArrayList<>(normalized);
  }

  private String serializeKeywords(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(keywords);
    } catch (IOException e) {
      throw new IllegalArgumentException("关键词格式错误");
    }
  }

  private List<String> parseKeywords(String raw) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
    } catch (IOException e) {
      return List.of();
    }
  }

  private String normalizeName(String name) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("分类名称不能为空");
    }
    String trimmed = name.trim();
    if (trimmed.length() > 20) {
      throw new IllegalArgumentException("分类名称不能超过 20 个字符");
    }
    return trimmed;
  }
}
