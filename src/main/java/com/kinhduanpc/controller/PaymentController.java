package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.repository.OrderRepository;
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
import java.util.Optional;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "API thanh toán")
public class PaymentController {

    private final VNPayService vnPayService;
    private final MoMoService momoService;
    private final ZaloPayService zaloPayService;
    private final OrderRepository orderRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo URL thanh toán VNPay
     * POST /api/payments/vnpay/create?orderId=123&amount=100000 (amount optional)
     */
    @PostMapping("/vnpay/create")
    @Operation(summary = "Tạo URL thanh toán VNPay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createVNPayPayment(
            @RequestParam Long orderId,
            @RequestParam(required = false) Long amount,
            HttpServletRequest request) {

        String ipAddress = getIpAddress(request);
        String paymentUrl = vnPayService.createPaymentUrl(orderId, amount, ipAddress);

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

        // Gọi handleCallback với bản copy để cập nhật DB luôn từ return URL
        // (IPN không reach được localhost trong dev; duplicate check trong handleCallback via transactionId)
        try {
            vnPayService.handleCallback(new java.util.HashMap<>(params));
        } catch (Exception e) {
            log.warn("handleCallback from return URL failed (non-critical): {}", e.getMessage());
        }

        Map<String, Object> result = vnPayService.handleReturn(new java.util.HashMap<>(params));

        try {
            String redirectUrl = frontendUrl + "/payment-result";
            redirectUrl += "?success=" + result.get("success");
            redirectUrl += "&message=" + java.net.URLEncoder.encode(result.get("message").toString(), "UTF-8");

            if (result.containsKey("orderCode")) {
                String orderCode = result.get("orderCode").toString();
                redirectUrl += "&orderCode=" + orderCode;
                Optional<Order> order = orderRepository.findByOrderCode(orderCode);
                if (order.isPresent()) {
                    redirectUrl += "&phone=" + java.net.URLEncoder.encode(order.get().getShippingPhone(), "UTF-8");
                }
            }
            if (result.containsKey("transactionId")) {
                redirectUrl += "&transactionId=" + result.get("transactionId");
            }

            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("Error encoding VNPay redirect URL", e);
            return new RedirectView(frontendUrl + "/payment-result?success=false&message=Error");
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
     * POST /api/payments/zalopay/create?orderId=123&amount=100000 (amount optional)
     */
    @PostMapping("/zalopay/create")
    @Operation(summary = "Tạo URL thanh toán ZaloPay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createZaloPayPayment(
            @RequestParam Long orderId,
            @RequestParam(required = false) Long amount,
            HttpServletRequest request) {

        String ipAddress = getIpAddress(request);
        String paymentUrl = zaloPayService.createPaymentUrl(orderId, amount, ipAddress);

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
     * ZaloPay Return URL - Redirect từ ZaloPay về frontend
     * GET /api/payments/zalopay/return?status=1&appid=...&apptransid=yyMMdd_orderId
     */
    @GetMapping("/zalopay/return")
    @Operation(summary = "ZaloPay Return URL")
    public RedirectView zaloPayReturn(@RequestParam Map<String, String> params) {
        log.info("Received ZaloPay return callback: {}", params);
        try {
            boolean success = "1".equals(params.get("status"));
            String appTransId = params.getOrDefault("apptransid", "");
            String redirectUrl = frontendUrl + "/payment-result";
            redirectUrl += "?success=" + success;
            redirectUrl += "&message=" + java.net.URLEncoder.encode(
                success ? "Thanh toán thành công" : "Thanh toán thất bại", "UTF-8");

            // appTransId format: yyMMdd_orderId → lấy orderId
            if (!appTransId.isBlank() && appTransId.contains("_")) {
                String[] parts = appTransId.split("_");
                if (parts.length == 2) {
                    try {
                        Long orderId = Long.parseLong(parts[1]);
                        Optional<Order> order = orderRepository.findById(orderId);
                        if (order.isPresent()) {
                            redirectUrl += "&orderCode=" + order.get().getOrderCode();
                            redirectUrl += "&phone=" + java.net.URLEncoder.encode(order.get().getShippingPhone(), "UTF-8");
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("Error encoding ZaloPay redirect URL", e);
            return new RedirectView(frontendUrl + "/payment-result?success=false&message=Error");
        }
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
