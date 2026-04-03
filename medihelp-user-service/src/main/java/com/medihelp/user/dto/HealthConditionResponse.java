package com.medihelp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthConditionResponse {
    private UUID id;
    private String conditionName;
    private LocalDate diagnosedDate;
    private String status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
