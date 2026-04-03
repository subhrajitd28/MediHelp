package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalTrendResponse {
    private String type;
    private Double averageValue;
    private Double minValue;
    private Double maxValue;
    private Long readingsCount;
    private Integer periodDays;
}
