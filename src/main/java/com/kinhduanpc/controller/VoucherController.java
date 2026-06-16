package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Voucher;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.VoucherRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers", description = "Mã giảm giá")
public class VoucherController {

    private final VoucherRepository voucherRepo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Voucher>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(voucherRepo.findAll()));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Voucher>> check(@RequestParam String code) {
        Voucher v = voucherRepo.findValidByCode(code, LocalDateTime.now())
            .orElseThrow(() -> AppException.badRequest("INVALID_VOUCHER", "Voucher không hợp lệ hoặc đã hết hạn"));
        return ResponseEntity.ok(ApiResponse.success(v));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Voucher>> create(@RequestBody Voucher voucher) {
        return ResponseEntity.ok(ApiResponse.success(voucherRepo.save(voucher)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Voucher>> update(@PathVariable Long id, @RequestBody Voucher req) {
        Voucher v = voucherRepo.findById(id).orElseThrow(() -> AppException.notFound("Voucher"));
        v.setName(req.getName());
        v.setDiscountValue(req.getDiscountValue());
        v.setMinOrderValue(req.getMinOrderValue());
        v.setEndDate(req.getEndDate());
        v.setIsActive(req.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(voucherRepo.save(v)));
    }
}
