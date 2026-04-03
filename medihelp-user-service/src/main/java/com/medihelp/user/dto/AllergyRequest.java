package com.medihelp.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyRequest {
    @NotBlank(message = "Allergen is required")
    private String allergen;
    private String severity;
    private String notes;
    private LocalDate diagnosedDate;
}
