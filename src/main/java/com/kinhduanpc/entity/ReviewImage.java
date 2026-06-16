package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
