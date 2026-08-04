package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.order.ReturnRequestDecisionRequest;
import com.kinhduanpc.dto.order.ReturnRequestRequest;
import com.kinhduanpc.dto.order.ReturnRequestResponse;
import com.kinhduanpc.dto.order.ReturnRequestReviewRequest;
import com.kinhduanpc.service.ReturnRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Return Requests", description = "Doi/tra hang trong 15 ngay")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/orders/{orderId}/return-requests")
    @Operation(summary = "Tạo yêu cầu đổi/trả cho đơn hàng đã giao (trong 15 ngày)")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> create(
            @PathVariable Long orderId,
            Authentication auth,
            @Valid @RequestBody ReturnRequestRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            returnRequestService.createReturnRequest(orderId, getUserId(auth), req),
            "Đã gửi yêu cầu đổi/trả"));
    }

    @GetMapping("/return-requests/my")
    @Operation(summary = "Danh sách yêu cầu đổi/trả của tôi")
    public ResponseEntity<ApiResponse<List<ReturnRequestResponse>>> getMy(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
            returnRequestService.getMyReturnRequests(getUserId(auth))));
    }

    @GetMapping("/return-requests/admin")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Danh sách tất cả yêu cầu đổi/trả")
    public ResponseEntity<ApiResponse<List<ReturnRequestResponse>>> getAdmin(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.getAdminReturnRequests(status)));
    }

    @PutMapping("/return-requests/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Chuyển yêu cầu sang trạng thái đang xem xét")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> review(
            @PathVariable Long id, @RequestBody(required = false) ReturnRequestReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
            returnRequestService.review(id, req != null ? req : new ReturnRequestReviewRequest()),
            "Đã chuyển sang xem xét"));
    }

    @PutMapping("/return-requests/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Duyệt/từ chối yêu cầu đổi/trả")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> decide(
            @PathVariable Long id, Authentication auth, @Valid @RequestBody ReturnRequestDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
            returnRequestService.decide(id, getUserId(auth), req), "Đã ghi nhận quyết định"));
    }

    @PutMapping("/return-requests/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Admin: Hoàn tất yêu cầu đổi/trả (áp dụng hoàn tiền nếu có)")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            returnRequestService.complete(id), "Đã hoàn tất yêu cầu đổi/trả"));
    }
}
