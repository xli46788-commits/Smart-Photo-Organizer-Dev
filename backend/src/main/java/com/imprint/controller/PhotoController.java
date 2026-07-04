package com.imprint.controller;

import com.imprint.common.ApiResponse;
import com.imprint.dto.PageResult;
import com.imprint.dto.PhotoVO;
import com.imprint.dto.UpdateCategoryRequest;
import com.imprint.service.PhotoService;
import com.imprint.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("files") MultipartFile[] files) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        List<PhotoVO> photos = photoService.uploadPhotos(userId, files);
        return ApiResponse.ok("上传成功", Map.of(
                "uploaded", photos.size(),
                "photos", photos
        ));
    }

    @GetMapping
    public ApiResponse<PageResult<PhotoVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return ApiResponse.ok(photoService.listPhotosPage(
                SecurityUtils.getCurrentUserId(), category, keyword, page, pageSize));
    }

    @GetMapping("/search")
    public ApiResponse<List<PhotoVO>> search(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(photoService.listPhotos(userId, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<PhotoVO> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(photoService.getPhoto(userId, id));
    }

    @PutMapping("/{id}/category")
    public ApiResponse<PhotoVO> updateCategory(@PathVariable Long id,
                                               @RequestBody UpdateCategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(photoService.updateCategory(userId, id, request.getCategory()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        photoService.deletePhoto(userId, id);
        return ApiResponse.ok("删除成功", null);
    }

    @PutMapping("/{id}/save")
    public ApiResponse<PhotoVO> saveEdited(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok("保存成功", photoService.saveEditedPhoto(userId, id, file));
    }
}
