package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionResponse {
    private UUID id;
    private String doctorName;
    private String hospital;
    private LocalDate prescribedDate;
    private String notes;
    private String ocrText;
    private Boolean ocrConfirmed;
    private String imageUrl;
    private Instant createdAt;
}
