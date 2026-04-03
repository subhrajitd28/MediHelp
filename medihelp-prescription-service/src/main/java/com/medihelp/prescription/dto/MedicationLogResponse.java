package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicationLogResponse {
    private UUID id;
    private UUID medicationId;
    private String drugName;
    private String status;
    private Instant scheduledTime;
    private Instant takenAt;
    private String notes;
    private Instant createdAt;
}
