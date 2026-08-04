package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.ProductRelated;
import com.kinhduanpc.entity.ProductStockByStore;
import com.kinhduanpc.entity.ProductView;
import com.kinhduanpc.entity.SearchHistory;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRelatedRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.ProductStockByStoreRepository;
import com.kinhduanpc.repository.ProductViewRepository;
import com.kinhduanpc.repository.SearchHistoryRepository;
import com.kinhduanpc.repository.TagRepository;
import com.kinhduanpc.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Quản lý sản phẩm")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepo;
    private final TagRepository tagRepo;
    private final ProductRelatedRepository productRelatedRepo;
    private final ProductStockByStoreRepository stockByStoreRepo;
    private final ProductViewRepository productViewRepo;
    private final SearchHistoryRepository searchHistoryRepo;

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
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "false") boolean track,
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Page<ProductResponse> result = productService.search(keyword, page, size);
        // track=true + page==0: chi ghi lich su khi la lan submit tim kiem thuc su
        // (khong ghi khi goi tu autocomplete go phim, phan trang, hay o cong cu admin).
        if (track && page == 0) {
            recordSearchHistory(keyword, (int) result.getTotalElements(), getUserId(auth), sessionId);
        }
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết sản phẩm theo slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(
            @PathVariable String slug,
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        ProductResponse product = productService.getBySlug(slug);
        recordProductView(product.getId(), getUserId(auth), sessionId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/recently-viewed")
    @Operation(summary = "Sản phẩm đã xem gần đây (tối đa 20, mới nhất trước)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRecentlyViewed(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Long userId = getUserId(auth);
        List<ProductView> raw = userId != null
            ? productViewRepo.findTop50ByUserIdOrderByViewedAtDesc(userId)
            : (sessionId != null ? productViewRepo.findTop50BySessionIdOrderByViewedAtDesc(sessionId) : List.of());

        // Dedupe theo san pham, giu lan xem gan nhat, gioi han 20 SP
        Map<Long, Product> distinctProducts = new LinkedHashMap<>();
        for (ProductView v : raw) {
            Product p = v.getProduct();
            distinctProducts.putIfAbsent(p.getId(), p);
            if (distinctProducts.size() >= 20) break;
        }

        List<ProductResponse> result = distinctProducts.values().stream()
            .map(productService::toSummaryResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }

    // Ghi log "fire-and-forget": khong duoc lam hong response chinh neu tracking loi.
    private void recordProductView(Long productId, Long userId, String sessionId) {
        if (userId == null && sessionId == null) return;
        try {
            boolean recentlyViewed = userId != null
                ? productViewRepo.findTopByProductIdAndUserIdOrderByViewedAtDesc(productId, userId)
                    .map(v -> v.getViewedAt().isAfter(LocalDateTime.now().minusMinutes(5))).orElse(false)
                : productViewRepo.findTopByProductIdAndSessionIdOrderByViewedAtDesc(productId, sessionId)
                    .map(v -> v.getViewedAt().isAfter(LocalDateTime.now().minusMinutes(5))).orElse(false);
            if (recentlyViewed) return;

            Product productRef = productRepo.getReferenceById(productId);
            productViewRepo.save(ProductView.builder()
                .product(productRef).userId(userId).sessionId(userId == null ? sessionId : null).build());
        } catch (Exception e) {
            log.warn("Khong ghi duoc product_views cho productId={}: {}", productId, e.getMessage());
        }
    }

    private void recordSearchHistory(String keyword, int resultCount, Long userId, String sessionId) {
        if (keyword == null || keyword.isBlank() || (userId == null && sessionId == null)) return;
        try {
            String trimmed = keyword.trim();
            String safeKeyword = trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
            searchHistoryRepo.save(SearchHistory.builder()
                .userId(userId).sessionId(userId == null ? sessionId : null)
                .keyword(safeKeyword).resultCount(resultCount).build());
        } catch (Exception e) {
            log.warn("Khong ghi duoc search_history cho keyword='{}': {}", keyword, e.getMessage());
        }
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Sản phẩm liên quan")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelated(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "false") boolean curatedOnly) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRelated(id, limit, curatedOnly)));
    }

    @GetMapping("/{id}/stock-by-store")
    @Operation(summary = "Danh sách showroom còn hàng cho sản phẩm này")
    public ResponseEntity<ApiResponse<List<ProductStockByStore>>> getStockByStore(@PathVariable Long id) {
        List<ProductStockByStore> stocks = stockByStoreRepo.findByProductId(id).stream()
            .filter(s -> s.getStockQty() > 0)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(stocks));
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
