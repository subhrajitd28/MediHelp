package com.medihelp.health.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.health.dto.BadgeResponse;
import com.medihelp.health.dto.StreakResponse;
import com.medihelp.health.service.BadgeService;
import com.medihelp.health.service.StreakService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "Streaks and badges")
public class GameController {

    private final StreakService streakService;
    private final BadgeService badgeService;

    @GetMapping("/streaks")
    public ResponseEntity<ApiResponse<List<StreakResponse>>> getStreaks(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(streakService.getStreaks(UUID.fromString(userId))));
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getBadges(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(badgeService.getUserBadges(UUID.fromString(userId))));
    }
}
