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
public class AllergyResponse {
    private UUID id;
    private String allergen;
    private String severity;
    private String notes;
    private LocalDate diagnosedDate;
    private Instant createdAt;
}
