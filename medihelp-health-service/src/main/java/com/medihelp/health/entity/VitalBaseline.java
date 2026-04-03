package com.medihelp.health.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "vital_baselines", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "vitalType"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String vitalType;

    @Builder.Default
    private Double runningMean = 0.0;

    @Builder.Default
    private Double runningVariance = 0.0;

    @Builder.Default
    private Long count = 0L;
}
