package com.medihelp.notification.service;

import com.medihelp.common.dto.PagedResponse;
import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.notification.dto.NotificationResponse;
import com.medihelp.notification.entity.Notification;
import com.medihelp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(UUID userId, String type, String category, String title, String message, String metadata) {
        Notification notification = Notification.builder()
                .userId(userId).type(type).category(category)
                .title(title).message(message).metadata(metadata)
                .build();
        return notificationRepository.save(notification);
    }

    public PagedResponse<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<NotificationResponse> content = notifPage.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.<NotificationResponse>builder()
                .content(content).page(notifPage.getNumber()).size(notifPage.getSize())
                .totalElements(notifPage.getTotalElements()).totalPages(notifPage.getTotalPages())
                .last(notifPage.isLast()).build();
    }

    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId).stream()
                .map(this::toResponse).toList();
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        n.setIsRead(true);
        n.setReadAt(Instant.now());
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).type(n.getType()).category(n.getCategory())
                .title(n.getTitle()).message(n.getMessage()).isRead(n.getIsRead())
                .metadata(n.getMetadata()).createdAt(n.getCreatedAt()).readAt(n.getReadAt())
                .build();
    }
}
