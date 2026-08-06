package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.ProductStockByStoreDTO;
import com.kinhduanpc.dto.StoreDTO;
import com.kinhduanpc.dto.StoreRequest;
import com.kinhduanpc.service.StoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreDTO>>> getAll(
            @RequestParam(required = false) String province) {
        return ResponseEntity.ok(ApiResponse.success(storeService.findAll(province)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<StoreDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(storeService.findBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreDTO>> create(@Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreDTO>> update(
            @PathVariable Long id, 
            @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeService.update(id, request)));
    }

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<ProductStockByStoreDTO>>> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storeService.getStockByStore(id)));
    }

    @PutMapping("/{id}/stock/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ProductStockByStoreDTO>> updateStock(
            @PathVariable Long id, 
            @PathVariable Long productId, 
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
            storeService.updateStock(id, productId, quantity)));
    }
}
