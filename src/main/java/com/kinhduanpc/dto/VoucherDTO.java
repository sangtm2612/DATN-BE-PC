package com.kinhduanpc.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer maxUsageCount;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
