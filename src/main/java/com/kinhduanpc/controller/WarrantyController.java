package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.warranty.*;
import com.kinhduanpc.service.WarrantyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warranties")
@RequiredArgsConstructor
@Tag(name = "Warranty", description = "Bảo hành & Yêu cầu sửa chữa")
public class WarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<WarrantyDTO>> lookup(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String orderCode) {

        if (serial != null) {
            return ResponseEntity.ok(ApiResponse.success(warrantyService.lookupBySerial(serial)));
        }
        if (orderCode != null) {
            return ResponseEntity.ok(ApiResponse.success(warrantyService.lookupByOrderCode(orderCode)));
        }
        throw com.kinhduanpc.exception.AppException.badRequest("MISSING_PARAM", 
                "Vui lòng nhập số serial hoặc mã đơn hàng");
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WarrantyDTO>>> getMyWarranties(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(warrantyService.getMyWarranties(userId)));
    }

    // ─── Service Requests (yeu cau sua chua) ───────────────────────────

    @PostMapping("/service-requests")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            Authentication auth,
            @Valid @RequestBody ServiceRequestRequest request) {

        Long userId = (Long) auth.getPrincipal();
        ServiceRequestResponse response = warrantyService.createServiceRequest(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(response, "Đã gửi yêu cầu sửa chữa: " + response.getServiceCode()));
    }

    @GetMapping("/service-requests/my")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getMyServiceRequests(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(warrantyService.getMyServiceRequests(userId)));
    }

    @GetMapping("/service-requests/admin")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAdminServiceRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long storeId) {

        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.getAdminServiceRequests(status, storeId)));
    }

    @GetMapping("/service-requests/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTechnicians() {
        return ResponseEntity.ok(ApiResponse.success(warrantyService.getTechnicians()));
    }

    @PutMapping("/service-requests/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','TECHNICIAN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> updateServiceRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequestStatusUpdateRequest request) {

        ServiceRequestResponse response = warrantyService.updateServiceRequestStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã cập nhật trạng thái"));
    }

    @PutMapping("/service-requests/{id}/approve-repair")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> approveRepair(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam boolean approved) {

        Long userId = (Long) auth.getPrincipal();
        ServiceRequestResponse response = warrantyService.approveRepair(id, userId, approved);
        
        return ResponseEntity.ok(ApiResponse.success(response,
                approved ? "Đã duyệt báo giá sửa chữa" : "Đã từ chối báo giá sửa chữa"));
    }
}
