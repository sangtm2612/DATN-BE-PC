package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Store;
import com.kinhduanpc.exception.AppException;
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
}
