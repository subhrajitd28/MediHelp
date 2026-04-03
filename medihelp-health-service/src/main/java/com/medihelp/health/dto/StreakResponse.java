package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakResponse {
    private String type;
    private Integer currentCount;
    private Integer longestCount;
    private LocalDate lastActivityDate;
    private boolean streakFreezeUsed;
}
