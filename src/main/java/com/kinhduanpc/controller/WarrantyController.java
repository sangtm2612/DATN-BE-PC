package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Warranty;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.WarrantyRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warranties")
@RequiredArgsConstructor
@Tag(name = "Warranty", description = "Tra cứu bảo hành")
public class WarrantyController {

    private final WarrantyRepository warrantyRepo;

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<Warranty>> lookup(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String orderCode) {

        if (serial != null) {
            return ResponseEntity.ok(ApiResponse.success(
                warrantyRepo.findBySerialNumber(serial)
                    .orElseThrow(() -> AppException.notFound("Thông tin bảo hành"))));
        }
        if (orderCode != null) {
            List<Warranty> ws = warrantyRepo.findByOrderCode(orderCode);
            if (ws.isEmpty()) throw AppException.notFound("Thông tin bảo hành");
            return ResponseEntity.ok(ApiResponse.success(ws.get(0)));
        }
        throw AppException.badRequest("MISSING_PARAM", "Vui lòng nhập số serial hoặc mã đơn hàng");
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Warranty>>> getMyWarranties(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(warrantyRepo.findByUserId(userId)));
    }
}
