package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.HealthConditionRequest;
import com.medihelp.user.dto.HealthConditionResponse;
import com.medihelp.user.service.HealthConditionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/conditions")
@RequiredArgsConstructor
@Tag(name = "Health Conditions", description = "Health condition management")
public class HealthConditionController {

    private final HealthConditionService conditionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthConditionResponse>>> getConditions(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(conditionService.getConditions(UUID.fromString(userId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthConditionResponse>> addCondition(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody HealthConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Condition added", conditionService.addCondition(UUID.fromString(userId), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthConditionResponse>> updateCondition(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody HealthConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Condition updated", conditionService.updateCondition(UUID.fromString(userId), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCondition(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        conditionService.deleteCondition(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Condition deleted", null));
    }
}
