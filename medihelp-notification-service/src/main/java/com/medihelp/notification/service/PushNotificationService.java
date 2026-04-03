package com.medihelp.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PushNotificationService {

    // TODO: Integrate Firebase FCM in Phase 3
    public void sendPush(String userId, String title, String body) {
        log.info("PUSH >> UserId: {}, Title: {}, Body: {}", userId, title, body);
    }
}
