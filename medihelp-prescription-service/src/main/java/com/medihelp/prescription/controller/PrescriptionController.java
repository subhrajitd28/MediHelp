package com.medihelp.prescription.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.prescription.dto.PrescriptionRequest;
import com.medihelp.prescription.dto.PrescriptionResponse;
import com.medihelp.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionResponse>> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Prescription created", prescriptionService.createPrescription(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> list(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescriptions(UUID.fromString(userId))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> get(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescription(UUID.fromString(userId), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> update(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id,
            @Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", prescriptionService.updatePrescription(UUID.fromString(userId), id, request)));
    }

    @PostMapping("/{id}/confirm-ocr")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> confirmOcr(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("OCR confirmed", prescriptionService.confirmOcr(UUID.fromString(userId), id, body.get("confirmedText"))));
    }
}
