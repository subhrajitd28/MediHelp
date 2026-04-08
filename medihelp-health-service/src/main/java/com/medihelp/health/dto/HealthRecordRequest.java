package com.medihelp.health.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HealthRecordRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String category;
    private String description;
    private String doctorName;
    private String hospital;
    private LocalDate recordDate;
    private String fileContentBase64;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
