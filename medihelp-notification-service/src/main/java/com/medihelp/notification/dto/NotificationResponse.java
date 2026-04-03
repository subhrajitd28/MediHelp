package com.medihelp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String type;
    private String category;
    private String title;
    private String message;
    private Boolean isRead;
    private String metadata;
    private Instant createdAt;
    private Instant readAt;
}
