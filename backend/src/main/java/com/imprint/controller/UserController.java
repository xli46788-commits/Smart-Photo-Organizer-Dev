package com.imprint.controller;

import com.imprint.common.ApiResponse;
import com.imprint.security.UserPrincipal;
import com.imprint.service.PhotoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final PhotoService photoService;

    public UserController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(@AuthenticationPrincipal UserPrincipal principal) {
        long photoCount = photoService.countByUser(principal.getId());
        return ApiResponse.ok(Map.of(
                "userId", principal.getId(),
                "username", principal.getUsername(),
                "photoCount", photoCount
        ));
    }
}
