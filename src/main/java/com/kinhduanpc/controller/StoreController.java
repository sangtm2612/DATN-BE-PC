package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.ProductStockByStore;
import com.kinhduanpc.entity.ProductStockByStoreId;
import com.kinhduanpc.entity.Store;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.ProductStockByStoreRepository;
import com.kinhduanpc.repository.StoreRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Hệ thống showroom")
public class StoreController {

    private final StoreRepository storeRepo;
    private final ProductStockByStoreRepository stockRepo;
    private final ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Store>>> getAll(
            @RequestParam(required = false) String province) {
        List<Store> stores = province != null
            ? storeRepo.findByProvinceAndIsActiveTrue(province)
            : storeRepo.findByIsActiveTrueOrderByProvinceAscNameAsc();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<Store>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
            storeRepo.findBySlug(slug).orElseThrow(() -> AppException.notFound("Cửa hàng"))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Store>> create(@RequestBody Store store) {
        return ResponseEntity.ok(ApiResponse.success(storeRepo.save(store)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Store>> update(@PathVariable Long id, @RequestBody Store req) {
        Store s = storeRepo.findById(id).orElseThrow(() -> AppException.notFound("Cửa hàng"));
        s.setName(req.getName()); s.setAddress(req.getAddress());
        s.setPhone(req.getPhone()); s.setEmail(req.getEmail());
        s.setOpenHours(req.getOpenHours()); s.setGoogleMapsUrl(req.getGoogleMapsUrl());
        return ResponseEntity.ok(ApiResponse.success(storeRepo.save(s)));
    }

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<ProductStockByStore>>> getStock(@PathVariable Long id) {
        if (!storeRepo.existsById(id)) throw AppException.notFound("Cửa hàng");
        return ResponseEntity.ok(ApiResponse.success(stockRepo.findByStoreId(id)));
    }

    @PutMapping("/{id}/stock/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ProductStockByStore>> updateStock(
            @PathVariable Long id, @PathVariable Long productId, @RequestParam int quantity) {
        Store store = storeRepo.findById(id).orElseThrow(() -> AppException.notFound("Cửa hàng"));
        Product product = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Sản phẩm"));

        ProductStockByStoreId key = new ProductStockByStoreId(productId, id);
        ProductStockByStore stock = stockRepo.findById(key)
            .orElse(ProductStockByStore.builder().product(product).store(store).build());
        stock.setStockQty(Math.max(0, quantity));
        return ResponseEntity.ok(ApiResponse.success(stockRepo.save(stock)));
    }
}
