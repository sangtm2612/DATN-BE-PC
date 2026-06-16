package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "oauth_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "oauth_id", nullable = false, length = 200)
    private String oauthId;

    @Column(length = 200)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
