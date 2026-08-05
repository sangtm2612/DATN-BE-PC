package com.kinhduanpc.service;

import com.kinhduanpc.entity.Notification;
import com.kinhduanpc.entity.Notification.NotificationType;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.repository.NotificationRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    @Async
    public void createNotification(Long userId, String type, String title,
                                    String message, String refType, Long refId) {
        userRepo.findById(userId).ifPresent(user -> {
            // Convert String to enum
            NotificationType notificationType;
            try {
                notificationType = NotificationType.valueOf(type);
            } catch (IllegalArgumentException e) {
                notificationType = NotificationType.system; // Default fallback
            }
            
            Notification n = Notification.builder()
                .user(user).type(notificationType).title(title).message(message)
                .referenceType(refType).referenceId(refId).isRead(false)
                .build();
            notificationRepo.save(n);
        });
    }

    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    public void markAllRead(Long userId) {
        notificationRepo.markAllAsRead(userId);
    }
}
