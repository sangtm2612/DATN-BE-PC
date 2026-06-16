package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryRepo.findRootCategories()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<Category>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
            categoryRepo.findBySlug(slug).orElseThrow(() -> AppException.notFound("Danh mục"))));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<Category>>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            categoryRepo.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success(categoryRepo.save(category)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> update(
            @PathVariable Long id, @RequestBody Category req) {
        Category c = categoryRepo.findById(id).orElseThrow(() -> AppException.notFound("Danh mục"));
        c.setName(req.getName());
        c.setIconUrl(req.getIconUrl());
        c.setImageUrl(req.getImageUrl());
        c.setDescription(req.getDescription());
        c.setIsActive(req.getIsActive());
        c.setSortOrder(req.getSortOrder());
        return ResponseEntity.ok(ApiResponse.success(categoryRepo.save(c)));
    }
}
