package com.medihelp.health.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_scores", indexes = {
    @Index(name = "idx_health_scores_user", columnList = "userId, calculatedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Builder.Default
    private Integer totalScore = 0;
    @Builder.Default
    private Integer medicationScore = 0;
    @Builder.Default
    private Integer vitalsScore = 0;
    @Builder.Default
    private Integer exerciseScore = 0;
    @Builder.Default
    private Integer dietScore = 0;
    @Builder.Default
    private Integer appointmentScore = 0;
    @Builder.Default
    private Integer moodScore = 0;

    @Column(nullable = false)
    private Instant calculatedAt;
}
