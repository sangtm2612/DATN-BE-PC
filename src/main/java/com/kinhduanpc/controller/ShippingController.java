package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.ShippingMethodDTO;
import com.kinhduanpc.dto.ShippingMethodRequest;
import com.kinhduanpc.service.ShippingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping-methods")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Phương thức giao hàng")
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingMethodDTO>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getActiveMethods()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ShippingMethodDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getAllMethods()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingMethodDTO>> create(
            @Valid @RequestBody ShippingMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(shippingService.create(request)));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingMethodDTO>> update(
            @PathVariable Long id, 
            @Valid @RequestBody ShippingMethodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.update(id, request)));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shippingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa phương thức giao hàng"));
    }
}
