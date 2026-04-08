package com.medihelp.health.entity;

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
@Table(name = "health_records", indexes = {
    @Index(name = "idx_health_records_user", columnList = "userId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    private String category; // TEST_REPORT, DOCTOR_NOTE, PRESCRIPTION, IMAGING, OTHER

    private String description;

    private String doctorName;
    private String hospital;
    private LocalDate recordDate;

    @Column(columnDefinition = "TEXT")
    private String fileContentBase64; // Base64 encoded file content

    private String fileName;
    private String fileType; // application/pdf, image/jpeg, etc.
    private Long fileSize;

    @CreationTimestamp
    private Instant createdAt;
}
