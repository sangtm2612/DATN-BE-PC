package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.VoucherDTO;
import com.kinhduanpc.dto.VoucherRequest;
import com.kinhduanpc.service.VoucherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ApiResponse<VoucherDTO>> check(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.checkVoucher(code)));
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
