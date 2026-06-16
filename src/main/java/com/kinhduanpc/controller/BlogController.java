package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.BlogCategory;
import com.kinhduanpc.entity.BlogPost;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BlogPostRepository;
import com.kinhduanpc.repository.BlogCategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Tin tức & Blog công nghệ")
public class BlogController {

    private final BlogPostRepository blogRepo;
    private final BlogCategoryRepository blogCategoryRepo;

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
    public ResponseEntity<ApiResponse<BlogPost>> getBySlug(@PathVariable String slug) {
        BlogPost post = blogRepo.findBySlugAndIsPublishedTrue(slug)
            .orElseThrow(() -> AppException.notFound("Bài viết"));
        blogRepo.incrementViewCount(post.getId());
        return ResponseEntity.ok(ApiResponse.success(post));
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
        return ResponseEntity.ok(ApiResponse.success(blogRepo.save(post)));
    }
}
