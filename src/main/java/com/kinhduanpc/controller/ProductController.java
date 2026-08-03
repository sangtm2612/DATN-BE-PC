package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.ProductRelated;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRelatedRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.TagRepository;
import com.kinhduanpc.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Quản lý sản phẩm")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepo;
    private final TagRepository tagRepo;
    private final ProductRelatedRepository productRelatedRepo;

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
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "false") boolean curatedOnly) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelated(id, limit, curatedOnly)));
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

    @PutMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional
    @Operation(summary = "Gán tag cho sản phẩm (Admin/Staff)")
    public ResponseEntity<ApiResponse<Void>> updateTags(
            @PathVariable Long id,
            @RequestBody(required = false) List<Long> tagIds) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));
        p.setTags(tagIds == null ? List.of() : tagRepo.findAllById(tagIds));
        productRepo.save(p);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật tag thành công"));
    }

    @PutMapping("/{id}/related")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional
    @Operation(summary = "Gán sản phẩm liên quan (Admin/Staff, tối đa 8)")
    public ResponseEntity<ApiResponse<Void>> updateRelated(
            @PathVariable Long id,
            @RequestBody(required = false) List<Long> relatedProductIds) {
        List<Long> ids = relatedProductIds == null
            ? List.of()
            : new ArrayList<>(new LinkedHashSet<>(relatedProductIds));

        if (ids.size() > 8) {
            throw AppException.badRequest("TOO_MANY_RELATED", "Tối đa 8 sản phẩm liên quan");
        }
        if (ids.contains(id)) {
            throw AppException.badRequest("SELF_REFERENCE", "Sản phẩm không thể liên quan tới chính nó");
        }

        Product p = productRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));
        List<Product> relatedProducts = productRepo.findAllById(ids);
        if (relatedProducts.size() != ids.size()) {
            throw AppException.badRequest("INVALID_RELATED_PRODUCT", "Một số sản phẩm liên quan không tồn tại");
        }
        Map<Long, Product> byId = relatedProducts.stream()
            .collect(Collectors.toMap(Product::getId, x -> x));

        productRelatedRepo.deleteByProductId(id);
        List<ProductRelated> toSave = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            toSave.add(ProductRelated.builder()
                .product(p)
                .relatedProduct(byId.get(ids.get(i)))
                .sortOrder(i)
                .build());
        }
        productRelatedRepo.saveAll(toSave);

        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật sản phẩm liên quan thành công"));
    }
}
