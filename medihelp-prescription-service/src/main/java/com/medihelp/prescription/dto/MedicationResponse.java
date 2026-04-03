package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicationResponse {
    private UUID id;
    private UUID prescriptionId;
    private String drugName;
    private String dosage;
    private String frequency;
    private String duration;
    private Boolean withFood;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private String notes;
    private List<MedicationScheduleResponse> schedules;
    private Instant createdAt;
}
