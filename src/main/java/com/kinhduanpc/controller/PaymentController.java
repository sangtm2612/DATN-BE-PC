package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.service.MoMoService;
import com.kinhduanpc.service.VNPayService;
import com.kinhduanpc.service.ZaloPayService;
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
    private final MoMoService momoService;
    private final ZaloPayService zaloPayService;

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

    // ─── MoMo Payment APIs ────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán MoMo
     * POST /api/payments/momo/create?orderId=123
     */
    @PostMapping("/momo/create")
    @Operation(summary = "Tạo URL thanh toán MoMo")
    public ResponseEntity<ApiResponse<Map<String, String>>> createMoMoPayment(
            @RequestParam Long orderId,
            HttpServletRequest request) {

        String ipAddress = getIpAddress(request);
        String paymentUrl = momoService.createPaymentUrl(orderId, ipAddress);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã tạo URL thanh toán MoMo"));
    }

    /**
     * MoMo IPN (Instant Payment Notification) - Callback từ MoMo server
     * POST /api/payments/momo/ipn
     */
    @PostMapping("/momo/ipn")
    @Operation(summary = "MoMo IPN Callback")
    public ResponseEntity<Map<String, Object>> momoIPN(@RequestBody Map<String, Object> params) {
        log.info("Received MoMo IPN callback");
        Map<String, Object> result = momoService.handleCallback(params);
        return ResponseEntity.ok(result);
    }

    /**
     * MoMo Return URL - Redirect từ MoMo về frontend
     * GET /api/payments/momo/return
     */
    @GetMapping("/momo/return")
    @Operation(summary = "MoMo Return URL")
    public RedirectView momoReturn(@RequestParam Map<String, Object> params) {
        log.info("Received MoMo return callback");
        Map<String, Object> result = momoService.handleReturn(params);

        // Build redirect URL to frontend
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

    // ─── ZaloPay Payment APIs ────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán ZaloPay
     * POST /api/payments/zalopay/create?orderId=123
     */
    @PostMapping("/zalopay/create")
    @Operation(summary = "Tạo URL thanh toán ZaloPay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createZaloPayPayment(
            @RequestParam Long orderId,
            HttpServletRequest request) {

        String ipAddress = getIpAddress(request);
        String paymentUrl = zaloPayService.createPaymentUrl(orderId, ipAddress);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã tạo URL thanh toán ZaloPay"));
    }

    /**
     * ZaloPay Callback - Callback từ ZaloPay server
     * POST /api/payments/zalopay/callback
     */
    @PostMapping("/zalopay/callback")
    @Operation(summary = "ZaloPay Callback")
    public ResponseEntity<Map<String, Object>> zaloPayCallback(@RequestBody Map<String, Object> params) {
        log.info("Received ZaloPay callback");
        Map<String, Object> result = zaloPayService.handleCallback(params);
        return ResponseEntity.ok(result);
    }

    /**
     * Query ZaloPay order status
     * GET /api/payments/zalopay/query?appTransId=yyMMdd_orderId
     */
    @GetMapping("/zalopay/query")
    @Operation(summary = "Query ZaloPay order status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryZaloPayOrder(
            @RequestParam String appTransId) {
        
        Map<String, Object> result = zaloPayService.queryOrderStatus(appTransId);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã query ZaloPay order"));
    }
}
