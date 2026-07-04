package com.imprint.controller;

import com.imprint.common.ApiResponse;
import com.imprint.dto.CategoryVO;
import com.imprint.dto.CreateCategoryRequest;
import com.imprint.dto.DeleteCategoryRequest;
import com.imprint.dto.UpdateCategoryKeywordsRequest;
import com.imprint.service.CategoryService;
import com.imprint.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public ApiResponse<List<CategoryVO>> list() {
    return ApiResponse.ok(categoryService.listCategories(SecurityUtils.getCurrentUserId()));
  }

  @PostMapping
  public ApiResponse<CategoryVO> create(@RequestBody CreateCategoryRequest request) {
    return ApiResponse.ok(categoryService.createCategory(
        SecurityUtils.getCurrentUserId(), request.getName(), request.getKeywords()));
  }

  @PutMapping("/{id}/keywords")
  public ApiResponse<CategoryVO> updateKeywords(@PathVariable Long id,
                                                @RequestBody UpdateCategoryKeywordsRequest request) {
    return ApiResponse.ok(categoryService.updateCategoryKeywords(
        SecurityUtils.getCurrentUserId(), id, request.getKeywords()));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id,
                                  @RequestBody DeleteCategoryRequest request) throws IOException {
    categoryService.deleteCategory(
        SecurityUtils.getCurrentUserId(), id, request.isDeletePhotos());
    return ApiResponse.ok("删除成功", null);
  }
}
