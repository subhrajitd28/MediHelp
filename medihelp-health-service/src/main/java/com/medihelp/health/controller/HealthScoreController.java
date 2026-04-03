package com.medihelp.health.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.common.dto.PagedResponse;
import com.medihelp.health.dto.HealthScoreResponse;
import com.medihelp.health.service.HealthScoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/score")
@RequiredArgsConstructor
@Tag(name = "Health Score", description = "Gamified health score")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthScoreResponse>> getLatestScore(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(healthScoreService.getLatestScore(UUID.fromString(userId))));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<HealthScoreResponse>>> getScoreHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(healthScoreService.getScoreHistory(UUID.fromString(userId), page, size)));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<HealthScoreResponse>> calculateScore(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success("Score calculated", healthScoreService.calculateScore(UUID.fromString(userId))));
    }
}
