package com.medihelp.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalsAnomalyEvent implements Serializable {

    private String userId;
    private String vitalType;
    private double recordedValue;
    private double threshold;
    private String anomalyType; // THRESHOLD or TREND
    private String severity; // LOW, MEDIUM, HIGH
    private Instant detectedAt;
}
