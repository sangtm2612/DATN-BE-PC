package com.kinhduanpc.dto.blog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinhduanpc.entity.BlogCategory;
import com.kinhduanpc.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogPostResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String thumbnailUrl;
    private Integer viewCount;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private String metaTitle;
    private String metaDesc;
    private BlogCategory blogCategory;
    private AuthorSummary author;
    private List<Product> mentionedProducts;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthorSummary {
        private Long id;
        private String fullName;
    }
}
