package com.medihelp.prescription.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.prescription.dto.*;
import com.medihelp.prescription.service.DrugInteractionService;
import com.medihelp.prescription.service.MedicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions/medications")
@RequiredArgsConstructor
@Tag(name = "Medications", description = "Medication management")
public class MedicationController {

    private final MedicationService medicationService;
    private final DrugInteractionService drugInteractionService;

    @PostMapping
    public ResponseEntity<ApiResponse<MedicationResponse>> add(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MedicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Medication added", medicationService.addMedication(UUID.fromString(userId), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationResponse>>> getActive(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.getActiveMedications(UUID.fromString(userId))));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<MedicationResponse>>> getAll(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(medicationService.getAllMedications(UUID.fromString(userId))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicationResponse>> update(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id,
            @Valid @RequestBody MedicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", medicationService.updateMedication(UUID.fromString(userId), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @RequestHeader("X-User-Id") String userId, @PathVariable UUID id) {
        medicationService.deactivateMedication(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Medication deactivated", null));
    }

    @PostMapping("/check-interactions")
    public ResponseEntity<ApiResponse<List<DrugInteractionResponse>>> checkInteractions(
            @RequestBody List<String> drugNames) {
        return ResponseEntity.ok(ApiResponse.success(drugInteractionService.checkInteractions(drugNames)));
    }
}
