package com.kinhduanpc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingMethodRequest {
    @NotBlank(message = "Tên phương thức không được để trống")
    private String name;
    
    private String description;
    
    @NotNull(message = "Phí giao hàng không được để trống")
    @Positive(message = "Phí giao hàng phải > 0")
    private BigDecimal baseFee;
    
    private BigDecimal freeThreshold;
    
    @NotBlank(message = "Thời gian dự kiến không được để trống")
    private String estimatedDays;
    
    private Boolean isActive;
}
