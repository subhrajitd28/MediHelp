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
public class HealthScoreResponse {
    private UUID id;
    private Integer totalScore;
    private Integer medicationScore;
    private Integer vitalsScore;
    private Integer exerciseScore;
    private Integer dietScore;
    private Integer appointmentScore;
    private Integer moodScore;
    private Instant calculatedAt;
}
