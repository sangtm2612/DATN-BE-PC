package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BlogCategory blogCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, unique = true, length = 350)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail", length = 500)
    private String thumbnailUrl;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "is_published", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "meta_title", length = 300)
    private String metaTitle;

    @Column(name = "meta_desc", length = 500)
    private String metaDesc;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
