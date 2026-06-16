package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Brand;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BrandRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Thương hiệu")
public class BrandController {

    private final BrandRepository brandRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Brand>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(brandRepo.findByIsActiveTrueOrderByNameAsc()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<Brand>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
            brandRepo.findBySlug(slug).orElseThrow(() -> AppException.notFound("Thương hiệu"))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Brand>> create(@RequestBody Brand brand) {
        return ResponseEntity.ok(ApiResponse.success(brandRepo.save(brand)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Brand>> update(@PathVariable Long id, @RequestBody Brand req) {
        Brand b = brandRepo.findById(id).orElseThrow(() -> AppException.notFound("Thương hiệu"));
        b.setName(req.getName());
        b.setLogoUrl(req.getLogoUrl());
        b.setWebsite(req.getWebsite());
        b.setDescription(req.getDescription());
        b.setIsActive(req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(brandRepo.save(b)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        brandRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa thương hiệu"));
    }
}
