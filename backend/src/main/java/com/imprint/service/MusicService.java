package com.imprint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.imprint.config.UploadProperties;
import com.imprint.dto.MusicVO;
import com.imprint.entity.UserMusic;
import com.imprint.mapper.UserMusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MusicService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            ".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac", ".webm"
    );

    private final UserMusicMapper userMusicMapper;
    private final UploadProperties uploadProperties;

    public MusicService(UserMusicMapper userMusicMapper, UploadProperties uploadProperties) {
        this.userMusicMapper = userMusicMapper;
        this.uploadProperties = uploadProperties;
    }

    public List<MusicVO> listMusic(Long userId) {
        return userMusicMapper.selectList(new LambdaQueryWrapper<UserMusic>()
                        .eq(UserMusic::getUserId, userId)
                        .orderByDesc(UserMusic::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<MusicVO> uploadMusic(Long userId, MultipartFile[] files) throws IOException {
        Path musicDir = Paths.get(uploadProperties.getDir(), String.valueOf(userId), "music")
                .toAbsolutePath().normalize();
        Files.createDirectories(musicDir);

        List<MusicVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validateAudioFile(file);

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path savedPath = musicDir.resolve(filename);
            Files.copy(file.getInputStream(), savedPath);

            UserMusic music = new UserMusic();
            music.setUserId(userId);
            music.setFilePath(toWebPath(userId, filename));
            music.setOriginalName(file.getOriginalFilename());
            music.setFileSize(file.getSize());
            music.setCreatedAt(LocalDateTime.now());
            userMusicMapper.insert(music);
            result.add(toVO(music));
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一个有效的音频文件");
        }
        return result;
    }

    public void deleteMusic(Long userId, Long musicId) throws IOException {
        UserMusic music = getOwnedMusic(userId, musicId);
        deletePhysicalFile(music.getFilePath());
        userMusicMapper.deleteById(musicId);
    }

    private UserMusic getOwnedMusic(Long userId, Long musicId) {
        UserMusic music = userMusicMapper.selectById(musicId);
        if (music == null || !music.getUserId().equals(userId)) {
            throw new IllegalArgumentException("音乐不存在");
        }
        return music;
    }

    private void validateAudioFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("audio/")) {
            return;
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不支持的音频格式: " + file.getOriginalFilename());
        }
    }

    private MusicVO toVO(UserMusic music) {
        MusicVO vo = new MusicVO();
        vo.setId(music.getId());
        vo.setOriginalName(music.getOriginalName());
        vo.setUrl(music.getFilePath());
        vo.setFileSize(music.getFileSize());
        vo.setCreatedAt(music.getCreatedAt());
        return vo;
    }

    private String toWebPath(Long userId, String filename) {
        return "/uploads/" + userId + "/music/" + filename;
    }

    private void deletePhysicalFile(String webPath) throws IOException {
        if (!StringUtils.hasText(webPath) || !webPath.startsWith("/uploads/")) {
            return;
        }
        String relative = webPath.substring("/uploads/".length());
        Path path = Paths.get(uploadProperties.getDir(), relative).toAbsolutePath().normalize();
        Files.deleteIfExists(path);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".mp3";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }
}
