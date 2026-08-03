package com.kinhduanpc.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String shortDesc;
    private String description;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Boolean isOnSale;
    private Integer discountPercent;
    private Integer stockQty;
    private Integer soldQty;
    private Integer viewCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer warrantyMonths;
    private String warrantyText;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isNew;
    private LocalDateTime createdAt;

    private CategoryInfo category;
    private BrandInfo brand;
    private List<ImageInfo> images;
    private List<AttributeGroup> attributeGroups;
    private List<String> tags;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class BrandInfo {
        private Long id;
        private String name;
        private String slug;
        private String logoUrl;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ImageInfo {
        private Long id;
        private String imageUrl;
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AttributeGroup {
        private String groupName;
        private List<AttributeItem> attributes;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AttributeItem {
        private String name;
        private String value;
        private String unit;
    }
}
