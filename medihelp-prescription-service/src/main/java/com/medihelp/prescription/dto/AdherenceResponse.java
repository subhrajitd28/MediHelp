package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdherenceResponse {
    private long totalDoses;
    private long takenCount;
    private long missedCount;
    private long skippedCount;
    private double adherencePercentage;
    private String period;
}
