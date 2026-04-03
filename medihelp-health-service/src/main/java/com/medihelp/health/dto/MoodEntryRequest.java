package com.medihelp.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodEntryRequest {
    @NotNull(message = "Mood rating is required")
    @Min(1) @Max(5)
    private Integer mood;
    private String journalText;
    private List<String> tags;
    private Double sleepHours;
    private Integer exerciseMinutes;
}
