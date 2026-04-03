package com.medihelp.health.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vitals", indexes = {
    @Index(name = "idx_vitals_user_type", columnList = "userId, type"),
    @Index(name = "idx_vitals_user_recorded", columnList = "userId, recordedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private String unit;

    @Builder.Default
    private String source = "MANUAL";

    private String notes;

    @Column(nullable = false)
    private Instant recordedAt;

    @CreationTimestamp
    private Instant createdAt;
}
