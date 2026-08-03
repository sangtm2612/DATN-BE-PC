package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_methods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "free_threshold", precision = 15, scale = 2)
    private BigDecimal freeThreshold;

    @Column(name = "estimated_days", length = 50)
    private String estimatedDays;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
