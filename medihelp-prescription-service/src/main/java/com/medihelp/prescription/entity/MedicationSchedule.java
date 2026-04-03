package com.medihelp.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalTime scheduledTime;

    private String dayOfWeek;

    @Builder.Default
    private Boolean isActive = true;
}
