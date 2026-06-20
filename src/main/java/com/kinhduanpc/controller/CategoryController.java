package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.category.CategoryResponse;
import com.kinhduanpc.entity.Category;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Danh mục sản phẩm")
public class CategoryController {

    private final CategoryRepository categoryRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<CategoryResponse> result = categoryRepo.findRootCategories()
            .stream()
            .map(CategoryResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(@PathVariable String slug) {
        Category c = categoryRepo.findBySlug(slug)
            .orElseThrow(() -> AppException.notFound("Danh mục"));
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(c)));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(@PathVariable Long id) {
        List<CategoryResponse> result = categoryRepo
            .findByParentIdAndIsActiveTrueOrderBySortOrderAsc(id)
            .stream()
            .map(CategoryResponse::fromFlat)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@RequestBody Category category) {
        Category saved = categoryRepo.save(category);
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.fromFlat(saved)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id, @RequestBody Category req) {
        Category c = categoryRepo.findById(id).orElseThrow(() -> AppException.notFound("Danh mục"));
        c.setName(req.getName());
        c.setIconUrl(req.getIconUrl());
        c.setImageUrl(req.getImageUrl());
        c.setDescription(req.getDescription());
        c.setIsActive(req.getIsActive());
        c.setSortOrder(req.getSortOrder());
        return ResponseEntity.ok(ApiResponse.success(CategoryResponse.from(categoryRepo.save(c))));
    }
}
