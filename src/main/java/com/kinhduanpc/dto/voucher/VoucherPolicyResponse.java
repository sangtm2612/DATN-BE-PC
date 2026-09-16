package com.kinhduanpc.dto.voucher;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherPolicyResponse {
    private Long id;
    private String name;
    private String description;
    private String triggerType;

    // Voucher info
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private String discountType;
    private BigDecimal discountValue;

    // Điều kiện
    private BigDecimal minTotalSpent;
    private Integer minCompletedOrders;
    private Integer minAccountAgeDays;
    private BigDecimal spendingMilestone;
    private Integer orderCountMilestone;

    // Giới hạn
    private Integer maxDistributions;
    private Integer distributedCount;
    private Boolean onePerUser;
    private Integer customExpiresDays;

    // Thời gian
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
