package com.medihelp.user.listener;

import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.common.event.UserRegisteredEvent;
import com.medihelp.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final UserProfileService profileService;

    @RabbitListener(queues = RabbitMQConfig.Q_PROFILE_USER_REGISTERED)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event for userId={}, email={}", event.getUserId(), event.getEmail());
        try {
            profileService.createProfile(
                    UUID.fromString(event.getUserId()),
                    event.getFirstName(),
                    event.getEmail()
            );
            log.info("Default profile created for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create profile for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
