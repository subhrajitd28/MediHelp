package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HealthRecordResponse {
    private UUID id;
    private String title;
    private String category;
    private String description;
    private String doctorName;
    private String hospital;
    private LocalDate recordDate;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private boolean hasFile;
    private Instant createdAt;
}
