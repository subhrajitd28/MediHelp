package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.SosResponse;
import com.medihelp.user.service.EmergencySosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/sos")
@RequiredArgsConstructor
@Tag(name = "Emergency SOS", description = "One-tap emergency alert to contacts")
public class EmergencySosController {

    private final EmergencySosService sosService;

    @PostMapping
    public ResponseEntity<ApiResponse<SosResponse>> triggerSos(
            @RequestHeader("X-User-Id") String userId) {
        SosResponse response = sosService.triggerSos(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
