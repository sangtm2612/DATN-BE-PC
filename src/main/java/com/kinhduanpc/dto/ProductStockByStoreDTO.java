package com.kinhduanpc.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductStockByStoreDTO {
    private Long productId;
    private String productName;
    private String productSlug;
    private String thumbnailUrl;
    private Long storeId;
    private String storeName;
    private Integer stockQty;
    private LocalDateTime updatedAt;
}
