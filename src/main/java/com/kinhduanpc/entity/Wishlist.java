package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(WishlistId.class)
public class Wishlist {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
