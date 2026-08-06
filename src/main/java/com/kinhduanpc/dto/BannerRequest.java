package com.kinhduanpc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerRequest {
    private String title;
    
    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imageUrl;
    
    private String linkUrl;
    
    @NotBlank(message = "Vị trí không được để trống")
    private String position;
    
    private Integer sortOrder;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
