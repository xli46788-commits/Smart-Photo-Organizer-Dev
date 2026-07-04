package com.imprint.controller;

import com.imprint.common.ApiResponse;
import com.imprint.dto.MusicVO;
import com.imprint.service.MusicService;
import com.imprint.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping
    public ApiResponse<List<MusicVO>> list() {
        return ApiResponse.ok(musicService.listMusic(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("files") MultipartFile[] files) throws IOException {
        List<MusicVO> tracks = musicService.uploadMusic(SecurityUtils.getCurrentUserId(), files);
        return ApiResponse.ok("上传成功", Map.of(
                "uploaded", tracks.size(),
                "tracks", tracks
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) throws IOException {
        musicService.deleteMusic(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.ok("删除成功", null);
    }
}
