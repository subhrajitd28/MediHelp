package com.medihelp.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drug_interaction_cache", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"drug1", "drug2"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteractionCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String drug1;

    @Column(nullable = false)
    private String drug2;

    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String source;
    private Instant lastCheckedAt;
}
