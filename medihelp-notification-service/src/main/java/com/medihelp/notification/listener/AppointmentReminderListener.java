package com.medihelp.notification.listener;

import com.medihelp.common.event.AppointmentReminderEvent;
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
public class AppointmentReminderListener {

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository prefRepository;
    private final EmailService emailService;
    private final PushNotificationService pushService;

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATION_APPOINTMENT)
    public void handleAppointmentReminder(AppointmentReminderEvent event) {
        log.info("Appointment reminder for user {} - Dr. {}", event.getUserId(), event.getDoctorName());
        UUID userId = UUID.fromString(event.getUserId());

        String title = "Upcoming appointment with Dr. " + event.getDoctorName();
        String message = "You have an appointment with Dr. " + event.getDoctorName()
                + " at " + event.getHospital() + " for " + event.getPurpose()
                + " on " + event.getScheduledAt();

        notificationService.createNotification(userId, "IN_APP", "APPOINTMENT_REMINDER", title, message,
                "{\"appointmentId\":\"" + event.getAppointmentId() + "\"}");

        NotificationPreference pref = prefRepository.findByUserId(userId).orElse(null);
        if (pref != null && Boolean.TRUE.equals(pref.getAppointmentReminders())) {
            emailService.sendEmail(event.getUserId(), title, message);
            if (Boolean.TRUE.equals(pref.getPushEnabled())) {
                pushService.sendPush(event.getUserId(), title, message);
            }
        }
    }
}
