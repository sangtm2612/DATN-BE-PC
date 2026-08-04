package com.kinhduanpc.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {

    private String promotionType; // general, build_pc, flash_sale, brand_deal, student, give_away

    @NotBlank
    private String name;
    private String description;

    @NotBlank
    private String discountType; // percent, fixed_amount, free_shipping
    @NotNull
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;

    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;
    private Boolean isActive;

    private BigDecimal buildpcMinCpuDiscountPct;
    private BigDecimal buildpcMaxCpuDiscountPct;
    private BigDecimal buildpcCashBonus;
    private BigDecimal buildpcMaxCashBonus;

    private List<Long> productIds;
    private List<Long> categoryIds;
    private List<Long> brandIds;
}
