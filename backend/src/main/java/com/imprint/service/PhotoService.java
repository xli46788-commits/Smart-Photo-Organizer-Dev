package com.imprint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drew.imaging.ImageMetadataReader;
import com.imprint.config.UploadProperties;
import com.imprint.dto.PageResult;
import com.imprint.dto.PhotoVO;
import com.imprint.entity.Photo;
import com.imprint.mapper.PhotoMapper;
import com.imprint.service.CategoryMapper.ClassifyResult;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoService {

    private final PhotoMapper photoMapper;
    private final UploadProperties uploadProperties;
    private final BaiduAiService baiduAiService;
    private final CategoryService categoryService;

    public PhotoService(PhotoMapper photoMapper, UploadProperties uploadProperties,
                        BaiduAiService baiduAiService, CategoryService categoryService) {
        this.photoMapper = photoMapper;
        this.uploadProperties = uploadProperties;
        this.baiduAiService = baiduAiService;
        this.categoryService = categoryService;
    }

    public List<PhotoVO> uploadPhotos(Long userId, MultipartFile[] files) throws IOException {
        Path userDir = Paths.get(uploadProperties.getDir(), String.valueOf(userId)).toAbsolutePath().normalize();
        Files.createDirectories(userDir);

        List<PhotoVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path savedPath = userDir.resolve(filename);
            Files.copy(file.getInputStream(), savedPath);

            String thumbFilename = "thumb_" + filename;
            Path thumbPath = userDir.resolve(thumbFilename);
            if (!createThumbnail(savedPath, thumbPath)) {
                Files.copy(savedPath, thumbPath);
            }

            ClassifyResult classifyResult = baiduAiService.classify(savedPath);

            Photo photo = new Photo();
            photo.setUserId(userId);
            photo.setFilePath(toWebPath(userId, filename));
            photo.setThumbPath(toWebPath(userId, thumbFilename));
            photo.setOriginalName(file.getOriginalFilename());
            photo.setAiCategory(categoryService.resolveForClassification(
                    userId, classifyResult, file.getOriginalFilename()));
            photo.setAiConfidence(classifyResult.getConfidence());
            photo.setTakenAt(extractTakenAt(savedPath));
            photo.setCreatedAt(LocalDateTime.now());
            photoMapper.insert(photo);

            result.add(toVO(photo));
        }
        return result;
    }

    public List<PhotoVO> listPhotos(Long userId, String category, String keyword) {
        return listPhotosFromWrapper(buildQueryWrapper(userId, category, keyword));
    }

    public PageResult<PhotoVO> listPhotosPage(Long userId, String category, String keyword, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<Photo> wrapper = buildQueryWrapper(userId, category, keyword);
        Page<Photo> pageParam = new Page<>(safePage, safePageSize);
        Page<Photo> result = photoMapper.selectPage(pageParam, wrapper);
        List<PhotoVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), safePage, safePageSize);
    }

    private LambdaQueryWrapper<Photo> buildQueryWrapper(Long userId, String category, String keyword) {
        LambdaQueryWrapper<Photo> wrapper = new LambdaQueryWrapper<Photo>()
                .eq(Photo::getUserId, userId)
                .orderByDesc(Photo::getCreatedAt);

        if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category) && !"全部".equals(category)) {
            wrapper.eq(Photo::getAiCategory, CategoryMapper.normalizeCategory(category));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Photo::getOriginalName, keyword)
                    .or().like(Photo::getAiCategory, keyword));
        }

        return wrapper;
    }

    private List<PhotoVO> listPhotosFromWrapper(LambdaQueryWrapper<Photo> wrapper) {
        return photoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public PhotoVO getPhoto(Long userId, Long photoId) {
        Photo photo = getOwnedPhoto(userId, photoId);
        return toVO(photo);
    }

    public PhotoVO updateCategory(Long userId, Long photoId, String category) {
        String normalized = CategoryMapper.normalizeCategory(category);
        categoryService.validateUserCategory(userId, normalized);
        Photo photo = getOwnedPhoto(userId, photoId);
        photo.setAiCategory(normalized);
        photoMapper.updateById(photo);
        return toVO(photo);
    }

    public void deletePhoto(Long userId, Long photoId) throws IOException {
        Photo photo = getOwnedPhoto(userId, photoId);
        deletePhysicalFile(photo.getFilePath());
        deletePhysicalFile(photo.getThumbPath());
        photoMapper.deleteById(photoId);
    }

    public PhotoVO saveEditedPhoto(Long userId, Long photoId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("编辑后的图片不能为空");
        }
        Photo photo = getOwnedPhoto(userId, photoId);
        Path savedPath = resolvePhysicalPath(photo.getFilePath());
        Path thumbPath = resolvePhysicalPath(photo.getThumbPath());
        Files.createDirectories(savedPath.getParent());
        Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);
        if (!createThumbnail(savedPath, thumbPath)) {
            Files.copy(savedPath, thumbPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return toVO(photo);
    }

    public long countByUser(Long userId) {
        return photoMapper.selectCount(new LambdaQueryWrapper<Photo>().eq(Photo::getUserId, userId));
    }

    private Photo getOwnedPhoto(Long userId, Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !photo.getUserId().equals(userId)) {
            throw new IllegalArgumentException("照片不存在");
        }
        return photo;
    }

    private PhotoVO toVO(Photo photo) {
        PhotoVO vo = new PhotoVO();
        vo.setId(photo.getId());
        vo.setOriginalName(photo.getOriginalName());
        vo.setUrl(photo.getFilePath());
        vo.setThumbUrl(photo.getThumbPath());
        vo.setAiCategory(CategoryMapper.normalizeCategory(photo.getAiCategory()));
        vo.setAiConfidence(photo.getAiConfidence());
        vo.setTakenAt(photo.getTakenAt());
        vo.setCreatedAt(photo.getCreatedAt());
        return vo;
    }

    private String toWebPath(Long userId, String filename) {
        return "/uploads/" + userId + "/" + filename;
    }

    private void deletePhysicalFile(String webPath) throws IOException {
        if (!StringUtils.hasText(webPath) || !webPath.startsWith("/uploads/")) {
            return;
        }
        Files.deleteIfExists(resolvePhysicalPath(webPath));
    }

    private Path resolvePhysicalPath(String webPath) {
        String relative = webPath.substring("/uploads/".length());
        return Paths.get(uploadProperties.getDir(), relative).toAbsolutePath().normalize();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private boolean createThumbnail(Path source, Path target) {
        try {
            Thumbnails.of(source.toFile())
                    .size(300, 300)
                    .keepAspectRatio(true)
                    .toFile(target.toFile());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDateTime extractTakenAt(Path path) {
        try {
            var metadata = ImageMetadataReader.readMetadata(path.toFile());
            var directory = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDateOriginal();
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
