package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.ShippingMethod;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ShippingMethodRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final ShippingMethodRepository shippingMethodRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingMethod>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepo.findByIsActiveTrue()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ShippingMethod>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepo.findAll()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingMethod>> create(@RequestBody ShippingMethod method) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shippingMethodRepo.save(method)));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingMethod>> update(@PathVariable Long id, @RequestBody ShippingMethod req) {
        ShippingMethod m = shippingMethodRepo.findById(id).orElseThrow(() -> AppException.notFound("Phương thức giao hàng"));
        m.setName(req.getName());
        m.setDescription(req.getDescription());
        m.setBaseFee(req.getBaseFee());
        m.setFreeThreshold(req.getFreeThreshold());
        m.setEstimatedDays(req.getEstimatedDays());
        m.setIsActive(req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepo.save(m)));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        if (!shippingMethodRepo.existsById(id)) throw AppException.notFound("Phương thức giao hàng");
        shippingMethodRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa phương thức giao hàng"));
    }
}
