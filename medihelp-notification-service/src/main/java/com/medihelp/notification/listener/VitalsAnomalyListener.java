package com.medihelp.notification.listener;

import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.common.event.VitalsAnomalyEvent;
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
public class VitalsAnomalyListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATION_ANOMALY)
    public void handleVitalsAnomaly(VitalsAnomalyEvent event) {
        log.warn("Vitals anomaly for user {} - {} = {} (threshold: {})", event.getUserId(), event.getVitalType(), event.getRecordedValue(), event.getThreshold());
        UUID userId = UUID.fromString(event.getUserId());

        String title = "Abnormal " + event.getVitalType() + " detected";
        String message = String.format("Your %s reading of %.1f exceeds normal range (threshold: %.1f). Severity: %s. Please consult a doctor if symptoms persist.",
                event.getVitalType(), event.getRecordedValue(), event.getThreshold(), event.getSeverity());

        notificationService.createNotification(userId, "IN_APP", "VITALS_ALERT", title, message, null);

        // Always send email for anomalies regardless of quiet hours
        emailService.sendEmail(event.getUserId(), "URGENT: " + title, message);
    }
}
