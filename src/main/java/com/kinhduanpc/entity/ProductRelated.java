package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_related")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ProductRelatedId.class)
public class ProductRelated {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_product_id", nullable = false)
    private Product relatedProduct;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
