package com.medihelp.health.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.health.dto.MoodEntryRequest;
import com.medihelp.health.dto.MoodEntryResponse;
import com.medihelp.health.service.MoodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/mood")
@RequiredArgsConstructor
@Tag(name = "Mood Journal", description = "Mental health mood tracking")
public class MoodController {

    private final MoodService moodService;

    @PostMapping
    public ResponseEntity<ApiResponse<MoodEntryResponse>> addMoodEntry(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MoodEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Mood entry added", moodService.addMoodEntry(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MoodEntryResponse>>> getMoodEntries(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(ApiResponse.success(moodService.getMoodEntries(UUID.fromString(userId), from, to)));
    }
}
