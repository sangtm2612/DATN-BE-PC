package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.order.*;
import com.kinhduanpc.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Đặt hàng")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            Authentication auth,
            @Valid @RequestBody CreateOrderRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(orderService.createOrder(userId, req), "Đặt hàng thành công"));
    }

    @GetMapping("/track")
    @Operation(summary = "Tra cứu đơn hàng (không cần đăng nhập)")
    public ResponseEntity<ApiResponse<OrderResponse>> track(
            @RequestParam String orderCode,
            @RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.success(orderService.trackOrder(orderCode, phone)));
    }

    @GetMapping
    @Operation(summary = "Lịch sử đơn hàng của tôi")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) auth.getPrincipal();
        Page<OrderResponse> result = orderService.getUserOrders(userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đơn hàng")
    public ResponseEntity<ApiResponse<OrderResponse>> getDetail(
            @PathVariable Long id, Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderDetail(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam(required = false) String reason) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(id, userId, reason)));
    }

    // Admin/Staff endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Danh sách tất cả đơn hàng")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderResponse> result = orderService.getUserOrders(null, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Cập nhật trạng thái đơn hàng")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String staffNote) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status, staffNote)));
    }
}
