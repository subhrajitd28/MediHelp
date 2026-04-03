package com.medihelp.notification.listener;

import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.common.event.UserRegisteredEvent;
import com.medihelp.notification.service.EmailService;
import com.medihelp.notification.service.NotificationPreferenceService;
import com.medihelp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.Q_NOTIFICATION_WELCOME)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event for {}", event.getEmail());
        UUID userId = UUID.fromString(event.getUserId());

        notificationService.createNotification(userId, "IN_APP", "WELCOME",
                "Welcome to MediHelp!",
                "Hi " + event.getFirstName() + ", welcome to MediHelp! Start by setting up your profile and adding your vitals.",
                null);

        preferenceService.createDefaults(userId);

        emailService.sendEmail(event.getEmail(), "Welcome to MediHelp!",
                "Hi " + event.getFirstName() + ", your MediHelp account is ready.");
    }
}
