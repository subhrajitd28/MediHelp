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
public class HealthConditionRequest {
    @NotBlank(message = "Condition name is required")
    private String conditionName;
    private LocalDate diagnosedDate;
    private String status;
    private String notes;
}
