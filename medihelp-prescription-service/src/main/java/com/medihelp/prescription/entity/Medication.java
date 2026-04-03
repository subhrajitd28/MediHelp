package com.medihelp.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String drugName;

    private String dosage;
    private String frequency;
    private String duration;

    @Builder.Default
    private Boolean withFood = false;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private Boolean isActive = true;

    private String notes;

    @CreationTimestamp
    private Instant createdAt;
}
