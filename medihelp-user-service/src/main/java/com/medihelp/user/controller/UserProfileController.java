package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.UserProfileRequest;
import com.medihelp.user.dto.UserProfileResponse;
import com.medihelp.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management")
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        UserProfileResponse profile = profileService.getProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UserProfileRequest request) {
        UserProfileResponse profile = profileService.updateProfile(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    @GetMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable UUID targetUserId) {
        UserProfileResponse profile = profileService.getProfile(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
