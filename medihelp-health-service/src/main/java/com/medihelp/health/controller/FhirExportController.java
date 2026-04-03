package com.medihelp.health.controller;

import com.medihelp.health.service.FhirExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/fhir")
@RequiredArgsConstructor
@Tag(name = "FHIR Export", description = "Export health data in FHIR R4 format")
public class FhirExportController {

    private final FhirExportService fhirExportService;

    @GetMapping(value = "/export", produces = "application/fhir+json")
    public ResponseEntity<String> exportFhirBundle(
            @RequestHeader("X-User-Id") String userId) {
        String bundle = fhirExportService.exportAsBundle(UUID.fromString(userId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(bundle);
    }
}
