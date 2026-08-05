package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "API thanh toán")
public class PaymentController {

    private final VNPayService vnPayService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo URL thanh toán VNPay
     * POST /api/payments/vnpay/create?orderId=123
     */
    @PostMapping("/vnpay/create")
    @Operation(summary = "Tạo URL thanh toán VNPay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createVNPayPayment(
            @RequestParam Long orderId,
            HttpServletRequest request) {

        String ipAddress = getIpAddress(request);
        String paymentUrl = vnPayService.createPaymentUrl(orderId, ipAddress);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã tạo URL thanh toán"));
    }

    /**
     * VNPay IPN (Instant Payment Notification) - Callback từ VNPay server
     * GET /api/payments/vnpay/ipn?vnp_TxnRef=...&vnp_Amount=...
     */
    @GetMapping("/vnpay/ipn")
    @Operation(summary = "VNPay IPN Callback")
    public ResponseEntity<Map<String, Object>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("Received VNPay IPN callback");
        Map<String, Object> result = vnPayService.handleCallback(params);
        return ResponseEntity.ok(result);
    }

    /**
     * VNPay Return URL - Redirect từ VNPay về frontend
     * GET /api/payments/vnpay/return?vnp_TxnRef=...&vnp_Amount=...
     */
    @GetMapping("/vnpay/return")
    @Operation(summary = "VNPay Return URL")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("Received VNPay return callback");
        Map<String, Object> result = vnPayService.handleReturn(params);

        // Build redirect URL to frontend with proper URL encoding
        try {
            String redirectUrl = frontendUrl + "/payment-result";
            redirectUrl += "?success=" + result.get("success");
            redirectUrl += "&message=" + java.net.URLEncoder.encode(result.get("message").toString(), "UTF-8");
            
            if (result.containsKey("orderCode")) {
                redirectUrl += "&orderCode=" + result.get("orderCode");
            }
            if (result.containsKey("transactionId")) {
                redirectUrl += "&transactionId=" + result.get("transactionId");
            }

            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("Error encoding redirect URL", e);
            String fallbackUrl = frontendUrl + "/payment-result?success=false&message=Error";
            return new RedirectView(fallbackUrl);
        }
    }

    /**
     * Get IP address từ request
     */
    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Nếu có nhiều IP (proxy chain), lấy IP đầu tiên
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress != null ? ipAddress : "0.0.0.0";
    }
}
