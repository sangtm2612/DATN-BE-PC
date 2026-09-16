package com.kinhduanpc.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String voucherType; // PUBLIC or PERSONAL
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer maxUsageCount;
    private Integer usagePerUser;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    
    // Fields for UserVoucher context (when returning user's vouchers)
    private String userVoucherStatus; // AVAILABLE, USED, EXPIRED
    private LocalDateTime assignedAt;
    private LocalDateTime usedAt;
    private LocalDateTime userVoucherExpiresAt;
}
