package com.kinhduanpc.dto.category;

import com.kinhduanpc.entity.Category;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String iconUrl;
    private String imageUrl;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private Long parentId;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category c) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSlug(c.getSlug());
        dto.setIconUrl(c.getIconUrl());
        dto.setImageUrl(c.getImageUrl());
        dto.setDescription(c.getDescription());
        dto.setSortOrder(c.getSortOrder());
        dto.setIsActive(c.getIsActive());
        // parentId — tránh load lazy nếu chưa init
        dto.setParentId(null); // set thủ công nếu cần
        // children — chỉ 1 level sâu để tránh N+1
        if (c.getChildren() != null && !c.getChildren().isEmpty()) {
            dto.setChildren(c.getChildren().stream()
                .map(CategoryResponse::fromFlat)
                .collect(Collectors.toList()));
        } else {
            dto.setChildren(List.of());
        }
        return dto;
    }

    /** Flat (không đệ quy children) — dùng cho level 2 trở xuống */
    public static CategoryResponse fromFlat(Category c) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSlug(c.getSlug());
        dto.setIconUrl(c.getIconUrl());
        dto.setImageUrl(c.getImageUrl());
        dto.setSortOrder(c.getSortOrder());
        dto.setIsActive(c.getIsActive());
        dto.setChildren(List.of());
        return dto;
    }
}
