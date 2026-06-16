package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attributes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_group", length = 100)
    private String attributeGroup;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Column(name = "attribute_value", nullable = false, length = 500)
    private String attributeValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
