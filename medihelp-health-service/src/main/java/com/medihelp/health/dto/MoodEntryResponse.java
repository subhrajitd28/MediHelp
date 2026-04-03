package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodEntryResponse {
    private String id;
    private Integer mood;
    private String journalText;
    private List<String> tags;
    private Double sleepHours;
    private Integer exerciseMinutes;
    private Instant recordedAt;
}
