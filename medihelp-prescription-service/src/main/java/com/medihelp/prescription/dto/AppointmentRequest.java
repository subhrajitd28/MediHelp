package com.medihelp.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentRequest {
    @NotBlank(message = "Doctor name is required")
    private String doctorName;
    private String hospital;
    private String specialization;
    private String purpose;
    @NotNull(message = "Scheduled time is required")
    private Instant scheduledAt;
    private String notes;
}
