package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Notification;
import com.kinhduanpc.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getAll(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getPrincipal();
        Page<Notification> result = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("count", notificationService.getUnreadCount(userId))));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đọc tất cả thông báo"));
    }
}
