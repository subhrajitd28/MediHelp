package com.medihelp.prescription.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.prescription.dto.AdherenceResponse;
import com.medihelp.prescription.dto.MedicationLogRequest;
import com.medihelp.prescription.dto.MedicationLogResponse;
import com.medihelp.prescription.service.MedicationLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions/medications/log")
@RequiredArgsConstructor
@Tag(name = "Medication Logs", description = "Medication adherence tracking")
public class MedicationLogController {

    private final MedicationLogService logService;

    @PostMapping
    public ResponseEntity<ApiResponse<MedicationLogResponse>> log(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MedicationLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Logged", logService.logMedication(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationLogResponse>>> getLogs(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(ApiResponse.success(logService.getLogs(UUID.fromString(userId), from, to)));
    }

    @GetMapping("/adherence")
    public ResponseEntity<ApiResponse<AdherenceResponse>> getAdherence(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(ApiResponse.success(logService.getAdherence(UUID.fromString(userId), from, to)));
    }
}
