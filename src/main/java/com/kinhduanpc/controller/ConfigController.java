package com.kinhduanpc.controller;

import com.kinhduanpc.config.OrderConfig;
import com.kinhduanpc.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API để lấy các cấu hình hệ thống cho frontend
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Tag(name = "Config", description = "API cấu hình hệ thống")
public class ConfigController {

    private final OrderConfig orderConfig;

    /**
     * Lấy cấu hình đơn hàng (order config)
     * GET /api/config/order
     */
    @GetMapping("/order")
    @Operation(summary = "Lấy cấu hình đơn hàng")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("codDepositAmount", orderConfig.getCodDepositAmount());
        config.put("autoCancelHours", orderConfig.getAutoCancelHours());
        config.put("autoCompleteDays", orderConfig.getAutoCompleteDays());
        config.put("returnDeadlineDays", orderConfig.getReturnDeadlineDays());
        
        return ResponseEntity.ok(ApiResponse.success(config, "Lấy cấu hình thành công"));
    }
}
