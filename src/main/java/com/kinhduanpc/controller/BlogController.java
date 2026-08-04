package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.blog.BlogPostResponse;
import com.kinhduanpc.entity.BlogCategory;
import com.kinhduanpc.entity.BlogPost;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BlogPostRepository;
import com.kinhduanpc.repository.BlogCategoryRepository;
import com.kinhduanpc.repository.ProductRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Tin tức & Blog công nghệ")
public class BlogController {

    private final BlogPostRepository blogRepo;
    private final BlogCategoryRepository blogCategoryRepo;
    private final ProductRepository productRepo;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<BlogCategory>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(blogCategoryRepo.findAll()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogPost>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> result;
        if (keyword != null) {
            result = blogRepo.searchByKeyword(keyword, pageable);
        } else if (categoryId != null) {
            result = blogRepo.findByBlogCategoryIdAndIsPublishedTrue(categoryId, pageable);
        } else {
            result = blogRepo.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostResponse>> getBySlug(@PathVariable String slug) {
        BlogPost post = blogRepo.findBySlugAndIsPublishedTrue(slug)
            .orElseThrow(() -> AppException.notFound("Bài viết"));
        blogRepo.incrementViewCount(post.getId());
        return ResponseEntity.ok(ApiResponse.success(toResponse(post)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<BlogPost>> create(@RequestBody BlogPost post) {
        return ResponseEntity.ok(ApiResponse.success(blogRepo.save(post)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<BlogPost>> update(
            @PathVariable Long id, @RequestBody BlogPost req) {
        BlogPost post = blogRepo.findById(id).orElseThrow(() -> AppException.notFound("Bài viết"));
        post.setTitle(req.getTitle()); post.setContent(req.getContent());
        post.setExcerpt(req.getExcerpt()); post.setThumbnailUrl(req.getThumbnailUrl());
        post.setIsPublished(req.getIsPublished());
        if (req.getBlogCategory() != null) post.setBlogCategory(req.getBlogCategory());
        return ResponseEntity.ok(ApiResponse.success(blogRepo.save(post)));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<Long>>> getMentionedProductIds(@PathVariable Long id) {
        BlogPost post = blogRepo.findById(id).orElseThrow(() -> AppException.notFound("Bài viết"));
        return ResponseEntity.ok(ApiResponse.success(
            post.getMentionedProducts().stream().map(p -> p.getId()).toList()));
    }

    @PutMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Void>> setMentionedProducts(
            @PathVariable Long id, @RequestBody List<Long> productIds) {
        BlogPost post = blogRepo.findById(id).orElseThrow(() -> AppException.notFound("Bài viết"));
        post.setMentionedProducts(productIds == null || productIds.isEmpty()
            ? new ArrayList<>()
            : productRepo.findAllById(productIds));
        blogRepo.save(post);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật sản phẩm liên quan"));
    }

    private BlogPostResponse toResponse(BlogPost post) {
        return BlogPostResponse.builder()
            .id(post.getId()).title(post.getTitle()).slug(post.getSlug())
            .excerpt(post.getExcerpt()).content(post.getContent())
            .thumbnailUrl(post.getThumbnailUrl()).viewCount(post.getViewCount())
            .isPublished(post.getIsPublished()).publishedAt(post.getPublishedAt())
            .metaTitle(post.getMetaTitle()).metaDesc(post.getMetaDesc())
            .blogCategory(post.getBlogCategory())
            .author(post.getAuthor() != null
                ? new BlogPostResponse.AuthorSummary(post.getAuthor().getId(), post.getAuthor().getFullName())
                : null)
            .mentionedProducts(post.getMentionedProducts())
            .build();
    }
}
