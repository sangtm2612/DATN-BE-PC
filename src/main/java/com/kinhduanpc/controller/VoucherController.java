package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.AssignVoucherRequest;
import com.kinhduanpc.dto.VoucherDTO;
import com.kinhduanpc.dto.VoucherRequest;
import com.kinhduanpc.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers", description = "Mã giảm giá")
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.findAll()));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<VoucherDTO>> check(
            @RequestParam String code,
            Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ResponseEntity.ok(ApiResponse.success(voucherService.checkVoucher(code, userId)));
    }

    @GetMapping("/my-vouchers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách voucher của tôi")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getMyVouchers(
            Authentication auth,
            @RequestParam(required = false) String status) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            voucherService.getMyVouchers(userId, status)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Phân phối voucher cho users",
               description = "Admin phân phối voucher PERSONAL cho danh sách users")
    public ResponseEntity<ApiResponse<String>> assignToUsers(
            @PathVariable Long id,
            @Valid @RequestBody AssignVoucherRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        voucherService.assignVoucherToUsers(id, request.getUserIds(),
            request.getExpiresAt(), userId);
        return ResponseEntity.ok(ApiResponse.success(
            "Đã phân phối voucher cho " + request.getUserIds().size() + " users"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherDTO>> create(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherDTO>> update(
            @PathVariable Long id, 
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.update(id, request)));
    }
}
