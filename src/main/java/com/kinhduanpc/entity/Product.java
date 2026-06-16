package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(nullable = false, unique = true, length = 350)
    private String slug;

    @Column(unique = true, length = 100)
    private String sku;

    @Column(name = "short_desc", columnDefinition = "TEXT")
    private String shortDesc;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "is_on_sale", nullable = false)
    @Builder.Default
    private Boolean isOnSale = false;

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private Integer stockQty = 0;

    @Column(name = "sold_qty", nullable = false)
    @Builder.Default
    private Integer soldQty = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(precision = 8, scale = 3)
    private BigDecimal weight;

    @Column(name = "warranty_months", nullable = false)
    @Builder.Default
    private Integer warrantyMonths = 12;

    @Column(name = "warranty_text", length = 200)
    private String warrantyText;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "meta_title", length = 300)
    private String metaTitle;

    @Column(name = "meta_desc", length = 500)
    private String metaDesc;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();
}
