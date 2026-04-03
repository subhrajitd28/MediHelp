package com.medihelp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPreferenceRequest {
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean medicationReminders;
    private Boolean anomalyAlerts;
    private Boolean appointmentReminders;
    private Boolean dailyDigest;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
}
