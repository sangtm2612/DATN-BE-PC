package com.kinhduanpc.dto.pcbuild;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PcBuildResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private List<ItemResponse> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private Long componentTypeId;
        private String componentTypeName;
        private Long productId;
        private String productName;
        private String productThumbnail;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
