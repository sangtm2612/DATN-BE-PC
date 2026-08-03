package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "result_count", nullable = false)
    @Builder.Default
    private Integer resultCount = 0;

    @CreationTimestamp
    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt;
}
