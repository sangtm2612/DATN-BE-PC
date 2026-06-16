package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "notification_type")
    private String type; // order_update, promotion, warranty_expiry, service_update, system

    @Column(nullable = false, length = 200)
    private String title;

    // Schema column: content
    @Column(name = "content", columnDefinition = "TEXT")
    private String message;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // order, warranty, service_request

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
