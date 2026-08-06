package com.kinhduanpc.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingMethodDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal baseFee;
    private BigDecimal freeThreshold;
    private String estimatedDays;
    private Boolean isActive;
}
