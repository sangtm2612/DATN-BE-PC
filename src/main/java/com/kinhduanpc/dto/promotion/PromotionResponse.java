package com.kinhduanpc.dto.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromotionResponse {
    private Long id;
    private String promotionType;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private BigDecimal buildpcMinCpuDiscountPct;
    private BigDecimal buildpcMaxCpuDiscountPct;
    private BigDecimal buildpcCashBonus;
    private BigDecimal buildpcMaxCashBonus;
    private List<Long> productIds;
    private List<Long> categoryIds;
    private List<Long> brandIds;
    private LocalDateTime createdAt;
}
