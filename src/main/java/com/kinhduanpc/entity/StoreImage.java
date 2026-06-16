package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
