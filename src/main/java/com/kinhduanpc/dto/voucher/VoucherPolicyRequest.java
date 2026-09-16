package com.kinhduanpc.dto.voucher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherPolicyRequest {

    @NotBlank(message = "Tên chính sách không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Loại sự kiện kích hoạt không được để trống")
    private String triggerType;

    @NotNull(message = "Voucher ID không được để trống")
    private Long voucherId;

    // Điều kiện
    private BigDecimal minTotalSpent;
    private Integer minCompletedOrders;
    private Integer minAccountAgeDays;
    private BigDecimal spendingMilestone;
    private Integer orderCountMilestone;

    // Giới hạn
    private Integer maxDistributions;
    private Boolean onePerUser;
    private Integer customExpiresDays;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private Boolean isActive;
}
