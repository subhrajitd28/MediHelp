package com.medihelp.notification.service;

import com.medihelp.notification.dto.NotificationPreferenceRequest;
import com.medihelp.notification.dto.NotificationPreferenceResponse;
import com.medihelp.notification.entity.NotificationPreference;
import com.medihelp.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository prefRepository;

    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreference pref = prefRepository.findByUserId(userId)
                .orElseGet(() -> createDefaults(userId));
        return toResponse(pref);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference pref = prefRepository.findByUserId(userId)
                .orElseGet(() -> createDefaults(userId));

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getMedicationReminders() != null) pref.setMedicationReminders(request.getMedicationReminders());
        if (request.getAnomalyAlerts() != null) pref.setAnomalyAlerts(request.getAnomalyAlerts());
        if (request.getAppointmentReminders() != null) pref.setAppointmentReminders(request.getAppointmentReminders());
        if (request.getDailyDigest() != null) pref.setDailyDigest(request.getDailyDigest());
        if (request.getQuietHoursStart() != null) pref.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) pref.setQuietHoursEnd(request.getQuietHoursEnd());

        return toResponse(prefRepository.save(pref));
    }

    public NotificationPreference createDefaults(UUID userId) {
        return prefRepository.save(NotificationPreference.builder().userId(userId).build());
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference p) {
        return NotificationPreferenceResponse.builder()
                .id(p.getId()).emailEnabled(p.getEmailEnabled()).pushEnabled(p.getPushEnabled())
                .medicationReminders(p.getMedicationReminders()).anomalyAlerts(p.getAnomalyAlerts())
                .appointmentReminders(p.getAppointmentReminders()).dailyDigest(p.getDailyDigest())
                .quietHoursStart(p.getQuietHoursStart()).quietHoursEnd(p.getQuietHoursEnd())
                .build();
    }
}
