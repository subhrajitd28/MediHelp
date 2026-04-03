package com.medihelp.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID userId;

    @Builder.Default
    private Boolean emailEnabled = true;
    @Builder.Default
    private Boolean pushEnabled = true;
    @Builder.Default
    private Boolean medicationReminders = true;
    @Builder.Default
    private Boolean anomalyAlerts = true;
    @Builder.Default
    private Boolean appointmentReminders = true;
    @Builder.Default
    private Boolean dailyDigest = false;

    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
