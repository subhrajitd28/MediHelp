package com.medihelp.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalRequest {
    @NotBlank(message = "Vital type is required")
    private String type;
    @NotNull(message = "Value is required")
    private Double value;
    @NotBlank(message = "Unit is required")
    private String unit;
    private String source;
    private String notes;
    private Instant recordedAt;
}
