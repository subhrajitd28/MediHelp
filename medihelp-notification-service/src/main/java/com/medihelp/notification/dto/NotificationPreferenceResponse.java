package com.medihelp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPreferenceResponse {
    private UUID id;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean medicationReminders;
    private Boolean anomalyAlerts;
    private Boolean appointmentReminders;
    private Boolean dailyDigest;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
}
