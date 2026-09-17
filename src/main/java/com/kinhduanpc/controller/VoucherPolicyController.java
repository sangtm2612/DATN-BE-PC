package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.voucher.VoucherPolicyRequest;
import com.kinhduanpc.dto.voucher.VoucherPolicyResponse;
import com.kinhduanpc.service.VoucherPolicyService;
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
@RequestMapping("/voucher-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Voucher Policies", description = "Quản lý chính sách phân phối voucher tự động")
public class VoucherPolicyController {

    private final VoucherPolicyService policyService;

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả chính sách voucher")
    public ResponseEntity<ApiResponse<List<VoucherPolicyResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(policyService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết chính sách voucher")
    public ResponseEntity<ApiResponse<VoucherPolicyResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(policyService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo chính sách phân phối voucher mới")
    public ResponseEntity<ApiResponse<VoucherPolicyResponse>> create(
            Authentication auth, @Valid @RequestBody VoucherPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(policyService.create(request, getUserId(auth))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật chính sách voucher")
    public ResponseEntity<ApiResponse<VoucherPolicyResponse>> update(
            @PathVariable Long id, Authentication auth,
            @Valid @RequestBody VoucherPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(policyService.update(id, request, getUserId(auth))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa chính sách voucher")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id, Authentication auth) {
        policyService.delete(id, getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Đã xóa chính sách voucher"));
    }
}
