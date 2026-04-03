package com.medihelp.notification.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.notification.dto.NotificationPreferenceRequest;
import com.medihelp.notification.dto.NotificationPreferenceResponse;
import com.medihelp.notification.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Notification preferences management")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(preferenceService.getPreferences(UUID.fromString(userId))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", preferenceService.updatePreferences(UUID.fromString(userId), request)));
    }
}
