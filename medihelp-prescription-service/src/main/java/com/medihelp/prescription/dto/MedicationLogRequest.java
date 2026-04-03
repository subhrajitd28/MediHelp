package com.medihelp.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicationLogRequest {
    @NotNull(message = "Medication ID is required")
    private UUID medicationId;
    @NotBlank(message = "Status is required")
    private String status;
    private Instant takenAt;
    private String notes;
}
