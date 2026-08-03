package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "installment_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false)
    private Integer months;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "min_order", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrder = new BigDecimal("3000000");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
