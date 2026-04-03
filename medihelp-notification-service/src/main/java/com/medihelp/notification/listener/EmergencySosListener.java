package com.medihelp.notification.listener;

import com.medihelp.common.event.EmergencySosEvent;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.notification.service.EmailService;
import com.medihelp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencySosListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATION_SOS)
    public void handleEmergencySos(EmergencySosEvent event) {
        log.warn("EMERGENCY SOS from user {} - notifying {} contacts", event.getUserId(), event.getEmergencyContacts().size());
        UUID userId = UUID.fromString(event.getUserId());

        // Create in-app notification
        notificationService.createNotification(userId, "IN_APP", "SYSTEM",
                "Emergency SOS Triggered",
                "Your SOS alert has been sent to " + event.getEmergencyContacts().size() + " emergency contact(s).",
                null);

        // Send email to each emergency contact
        for (EmergencySosEvent.ContactInfo contact : event.getEmergencyContacts()) {
            if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
                emailService.sendEmail(
                        contact.getEmail(),
                        "EMERGENCY: " + event.getUserName() + " needs help!",
                        event.getMedicalSummaryHtml()
                );
                log.warn("SOS email sent to {} ({})", contact.getName(), contact.getEmail());
            }
        }
    }
}
