package com.kinhduanpc.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String excerpt;
    
    @NotBlank(message = "Nội dung không được để trống")
    private String content;
    
    private String thumbnailUrl;
    
    private Boolean isPublished;
    
    private LocalDateTime publishedAt;
    
    private String metaTitle;
    
    private String metaDesc;
    
    @NotNull(message = "Danh mục không được để trống")
    private Long blogCategoryId;
    
    private List<Long> mentionedProductIds;
}
