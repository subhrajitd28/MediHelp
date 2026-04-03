package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicationScheduleResponse {
    private UUID id;
    private UUID medicationId;
    private String drugName;
    private String dosage;
    private LocalTime scheduledTime;
    private String dayOfWeek;
    private Boolean withFood;
}
