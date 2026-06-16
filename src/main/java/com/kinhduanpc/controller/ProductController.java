package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Quản lý sản phẩm")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm với lọc & sắp xếp")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        Page<ProductResponse> result = productService.getProducts(categoryId, brandId, minPrice, maxPrice, sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm sản phẩm")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        Page<ProductResponse> result = productService.search(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết sản phẩm theo slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getBySlug(slug)));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Sản phẩm liên quan")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelated(id, limit)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo sản phẩm mới (Admin/Staff)")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @RequestBody ProductService.ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.createProduct(req), "Tạo sản phẩm thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật sản phẩm (Admin/Staff)")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @RequestBody ProductService.ProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, req)));
    }
}
