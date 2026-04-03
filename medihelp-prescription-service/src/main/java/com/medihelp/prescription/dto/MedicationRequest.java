package com.medihelp.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicationRequest {
    private UUID prescriptionId;
    @NotBlank(message = "Drug name is required")
    private String drugName;
    private String dosage;
    private String frequency;
    private String duration;
    private Boolean withFood;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
