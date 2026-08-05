package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.admin.RevenuePointResponse;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.service.AdminService;
import com.kinhduanpc.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Admin", description = "Tổng quan hệ thống")
public class AdminController {

    private final AdminService adminService;
    private final ProductService productService;
    private final ProductRepository productRepo;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<List<RevenuePointResponse>>> getRevenueChart(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRevenueChart(period)));
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock() {
        List<ProductResponse> products = productRepo.findLowStockProducts()
            .stream().map(productService::toSummaryResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
