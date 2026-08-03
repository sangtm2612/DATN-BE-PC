package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "promotion_type", nullable = false, columnDefinition = "promotion_type")
    @Builder.Default
    private PromotionType promotionType = PromotionType.general;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "discount_type", nullable = false, columnDefinition = "discount_type")
    private Voucher.DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "buildpc_min_cpu_discount_pct", precision = 5, scale = 2)
    private BigDecimal buildpcMinCpuDiscountPct;

    @Column(name = "buildpc_max_cpu_discount_pct", precision = 5, scale = 2)
    private BigDecimal buildpcMaxCpuDiscountPct;

    @Column(name = "buildpc_cash_bonus", precision = 15, scale = 2)
    private BigDecimal buildpcCashBonus;

    @Column(name = "buildpc_max_cash_bonus", precision = 15, scale = 2)
    private BigDecimal buildpcMaxCashBonus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(name = "promotion_products",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"))
    private List<Product> products = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "promotion_categories",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> categories = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "promotion_brands",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "brand_id"))
    private List<Brand> brands = new ArrayList<>();

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && now.isAfter(startDate) && now.isBefore(endDate);
    }

    public enum PromotionType { general, build_pc, student, brand_deal, give_away, flash_sale }
}
