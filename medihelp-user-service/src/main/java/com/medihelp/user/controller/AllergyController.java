package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.AllergyRequest;
import com.medihelp.user.dto.AllergyResponse;
import com.medihelp.user.service.AllergyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/allergies")
@RequiredArgsConstructor
@Tag(name = "Allergies", description = "Allergy management")
public class AllergyController {

    private final AllergyService allergyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AllergyResponse>>> getAllergies(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(allergyService.getAllergies(UUID.fromString(userId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AllergyResponse>> addAllergy(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AllergyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Allergy added", allergyService.addAllergy(UUID.fromString(userId), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AllergyResponse>> updateAllergy(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody AllergyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Allergy updated", allergyService.updateAllergy(UUID.fromString(userId), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAllergy(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        allergyService.deleteAllergy(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Allergy deleted", null));
    }
}
