package com.medihelp.user.controller;

import com.medihelp.common.dto.ApiResponse;
import com.medihelp.user.dto.SosRequest;
import com.medihelp.user.dto.SosResponse;
import com.medihelp.user.service.EmergencySosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Emergency SOS", description = "One-tap emergency alert to contacts")
public class EmergencySosController {

    private final EmergencySosService sosService;

    @PostMapping("/api/v1/users/me/sos")
    public ResponseEntity<ApiResponse<SosResponse>> triggerSos(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) SosRequest request) {
        SosResponse response = sosService.triggerSos(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping(value = "/api/v1/public/sos/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewShareableSummary(@PathVariable String token) {
        String html = sosService.getShareableSummary(token);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        String page = "<!DOCTYPE html><html><head><title>Emergency Medical Summary</title>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{font-family:Arial,sans-serif;max-width:600px;margin:40px auto;padding:20px;}</style>"
                + "</head><body>" + html + "</body></html>";
        return ResponseEntity.ok(page);
    }
}
