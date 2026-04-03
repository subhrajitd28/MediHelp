package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentResponse {
    private UUID id;
    private String doctorName;
    private String hospital;
    private String specialization;
    private String purpose;
    private Instant scheduledAt;
    private String status;
    private String notes;
    private Instant createdAt;
}
