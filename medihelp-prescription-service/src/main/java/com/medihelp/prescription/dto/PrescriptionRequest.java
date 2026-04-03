package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionRequest {
    private String doctorName;
    private String hospital;
    private LocalDate prescribedDate;
    private String notes;
    private String ocrText;
    private String imageUrl;
}
