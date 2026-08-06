package com.kinhduanpc.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
    
    private Long parentId;
    private String iconUrl;
    private String imageUrl;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
}
