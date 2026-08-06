package com.kinhduanpc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;
    
    @NotBlank(message = "Tên voucher không được để trống")
    private String name;
    
    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType; // PERCENTAGE or FIXED
    
    @NotNull(message = "Giá trị giảm không được để trống")
    @Positive(message = "Giá trị giảm phải > 0")
    private BigDecimal discountValue;
    
    private BigDecimal minOrderValue;
    private Integer maxUsageCount;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
    
    private Boolean isActive;
}
