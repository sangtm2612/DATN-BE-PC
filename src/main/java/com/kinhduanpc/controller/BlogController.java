package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.blog.BlogCategoryDTO;
import com.kinhduanpc.dto.blog.BlogPostRequest;
import com.kinhduanpc.dto.blog.BlogPostResponse;
import com.kinhduanpc.service.BlogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Tin tức & Blog công nghệ")
public class BlogController {

    private final BlogService blogService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<BlogCategoryDTO>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(blogService.getAllCategories()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogPostResponse>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<BlogPostResponse> result = blogService.getAllPosts(categoryId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
                new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> create(
            @Valid @RequestBody BlogPostRequest request,
            Authentication auth) {
        Long authorId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(blogService.createPost(request, authorId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BlogPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(blogService.updatePost(id, request)));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<Long>>> getMentionedProductIds(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getMentionedProductIds(id)));
    }

    @PutMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Void>> setMentionedProducts(
            @PathVariable Long id,
            @RequestBody List<Long> productIds) {
        blogService.setMentionedProducts(id, productIds);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã cập nhật sản phẩm liên quan"));
    }
}
