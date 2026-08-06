package com.kinhduanpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {
    
    // Product info
    private Long productId;
    private String productName;
    private String slug;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String thumbnailUrl;
    private Boolean inStock;
    private Integer stockQuantity;
    
    // Wishlist info
    private LocalDateTime addedAt;
}
