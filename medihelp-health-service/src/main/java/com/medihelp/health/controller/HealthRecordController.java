package com.medihelp.health.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.health.dto.HealthRecordRequest;
import com.medihelp.health.dto.HealthRecordResponse;
import com.medihelp.health.service.HealthRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/records")
@RequiredArgsConstructor
@Tag(name = "Health Records", description = "Medical document management")
public class HealthRecordController {

    private final HealthRecordService recordService;

    @PostMapping
    public ResponseEntity<ApiResponse<HealthRecordResponse>> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody HealthRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Record created",
                recordService.createRecord(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthRecordResponse>>> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String category) {
        List<HealthRecordResponse> records = category != null
                ? recordService.getRecordsByCategory(UUID.fromString(userId), category)
                : recordService.getRecords(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFile(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        String content = recordService.getFileContent(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("fileContentBase64", content != null ? content : "")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        recordService.deleteRecord(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Record deleted", null));
    }
}
