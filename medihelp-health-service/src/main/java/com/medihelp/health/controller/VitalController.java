package com.medihelp.health.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.common.dto.PagedResponse;
import com.medihelp.health.dto.VitalRequest;
import com.medihelp.health.dto.VitalResponse;
import com.medihelp.health.dto.VitalTrendResponse;
import com.medihelp.health.service.VitalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/vitals")
@RequiredArgsConstructor
@Tag(name = "Vitals", description = "Vital signs tracking")
public class VitalController {

    private final VitalService vitalService;

    @PostMapping
    public ResponseEntity<ApiResponse<VitalResponse>> recordVital(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody VitalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vital recorded", vitalService.recordVital(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<VitalResponse>>> getVitals(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(vitalService.getVitals(UUID.fromString(userId), type, from, to, page, size)));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Map<String, VitalResponse>>> getLatestVitals(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(vitalService.getLatestVitals(UUID.fromString(userId))));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<VitalTrendResponse>> getVitalTrends(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String type,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(vitalService.getVitalTrends(UUID.fromString(userId), type, days)));
    }
}
