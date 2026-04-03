package com.medihelp.notification.listener;

import com.medihelp.common.event.MedicationReminderEvent;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.notification.entity.NotificationPreference;
import com.medihelp.notification.repository.NotificationPreferenceRepository;
import com.medihelp.notification.service.EmailService;
import com.medihelp.notification.service.NotificationService;
import com.medihelp.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationReminderListener {

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository prefRepository;
    private final EmailService emailService;
    private final PushNotificationService pushService;

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATION_MEDICATION)
    public void handleMedicationReminder(MedicationReminderEvent event) {
        log.info("Medication reminder for user {} - {}", event.getUserId(), event.getDrugName());
        UUID userId = UUID.fromString(event.getUserId());

        String title = "Time to take " + event.getDrugName();
        String message = "Take " + event.getDrugName() + " (" + event.getDosage() + ")"
                + (event.isWithFood() ? " with food" : "");

        notificationService.createNotification(userId, "IN_APP", "MEDICATION_REMINDER", title, message,
                "{\"medicationId\":\"" + event.getMedicationId() + "\"}");

        NotificationPreference pref = prefRepository.findByUserId(userId).orElse(null);
        if (pref != null && Boolean.TRUE.equals(pref.getMedicationReminders())) {
            if (Boolean.TRUE.equals(pref.getPushEnabled())) {
                pushService.sendPush(event.getUserId(), title, message);
            }
        }
    }
}
