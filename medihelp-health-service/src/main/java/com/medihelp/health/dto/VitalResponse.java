package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalResponse {
    private UUID id;
    private String type;
    private Double value;
    private String unit;
    private String source;
    private String notes;
    private Instant recordedAt;
    private Instant createdAt;
}
