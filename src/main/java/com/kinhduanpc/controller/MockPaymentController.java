package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.Payment;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MOCK Payment Controller - CHỈ DÙNG CHO DEVELOPMENT
 * Giả lập thanh toán thành công mà không cần MoMo/VNPay thật
 */
@RestController
@RequestMapping("/mock/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mock Payment", description = "Mock payment cho development - CHỈ DÙNG TEST")
@Profile({"dev", "local"}) // Chỉ active trong dev environment
public class MockPaymentController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Mock thanh toán thành công cho MoMo
     * POST /api/mock/payments/momo/success?orderId=123
     */
    @PostMapping("/momo/success")
    @Operation(summary = "Mock MoMo payment thành công (DEV ONLY)")
    public ResponseEntity<ApiResponse<Map<String, String>>> mockMoMoSuccess(
            @RequestParam Long orderId) {
        
        log.warn("🚨 MOCK PAYMENT - DEV ONLY - Order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        // Update order payment status
        order.setPaymentStatus(Order.PaymentStatus.paid);
        orderRepository.save(order);

        // Create mock payment record
        String mockTransactionId = "MOCK_MOMO_" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(mockTransactionId)
                .gateway("momo_mock")
                .amount(order.getTotalAmount())
                .currency("VND")
                .status(Order.PaymentStatus.paid)
                .gatewayResponse("{\"mock\": true, \"message\": \"Development mock payment\"}")
                .paidAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock payment thành công");
        response.put("orderCode", order.getOrderCode());
        response.put("transactionId", mockTransactionId);
        response.put("redirectUrl", frontendUrl + "/payment-result?success=true&orderCode=" + order.getOrderCode() + "&transactionId=" + mockTransactionId + "&message=Thanh%20toan%20thanh%20cong");

        return ResponseEntity.ok(ApiResponse.success(response, "Mock payment thành công"));
    }

    /**
     * Mock thanh toán thất bại
     */
    @PostMapping("/momo/failed")
    @Operation(summary = "Mock MoMo payment thất bại (DEV ONLY)")
    public ResponseEntity<ApiResponse<Map<String, String>>> mockMoMoFailed(
            @RequestParam Long orderId) {
        
        log.warn("🚨 MOCK PAYMENT FAILED - DEV ONLY - Order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        // Update order payment status
        order.setPaymentStatus(Order.PaymentStatus.failed);
        orderRepository.save(order);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock payment thất bại");
        response.put("orderCode", order.getOrderCode());
        response.put("redirectUrl", frontendUrl + "/payment-result?success=false&orderCode=" + order.getOrderCode() + "&message=Thanh%20toan%20that%20bai");

        return ResponseEntity.ok(ApiResponse.success(response, "Mock payment thất bại"));
    }

    /**
     * Mock thanh toán thành công cho VNPay
     */
    @PostMapping("/vnpay/success")
    @Operation(summary = "Mock VNPay payment thành công (DEV ONLY)")
    public ResponseEntity<ApiResponse<Map<String, String>>> mockVNPaySuccess(
            @RequestParam Long orderId) {
        
        log.warn("🚨 MOCK PAYMENT - DEV ONLY - Order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        order.setPaymentStatus(Order.PaymentStatus.paid);
        orderRepository.save(order);

        String mockTransactionId = "MOCK_VNP_" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(mockTransactionId)
                .gateway("vnpay_mock")
                .amount(order.getTotalAmount())
                .currency("VND")
                .status(Order.PaymentStatus.paid)
                .gatewayResponse("{\"mock\": true}")
                .paidAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock payment thành công");
        response.put("orderCode", order.getOrderCode());
        response.put("transactionId", mockTransactionId);
        response.put("redirectUrl", frontendUrl + "/payment-result?success=true&orderCode=" + order.getOrderCode() + "&transactionId=" + mockTransactionId);

        return ResponseEntity.ok(ApiResponse.success(response, "Mock payment thành công"));
    }

    /**
     * Mock thanh toán thành công cho ZaloPay
     */
    @PostMapping("/zalopay/success")
    @Operation(summary = "Mock ZaloPay payment thành công (DEV ONLY)")
    public ResponseEntity<ApiResponse<Map<String, String>>> mockZaloPaySuccess(
            @RequestParam Long orderId) {
        
        log.warn("🚨 MOCK PAYMENT - DEV ONLY - Order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        order.setPaymentStatus(Order.PaymentStatus.paid);
        orderRepository.save(order);

        String mockTransactionId = "MOCK_ZLP_" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(mockTransactionId)
                .gateway("zalopay_mock")
                .amount(order.getTotalAmount())
                .currency("VND")
                .status(Order.PaymentStatus.paid)
                .gatewayResponse("{\"mock\": true, \"message\": \"Development mock payment\"}")
                .paidAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock payment thành công");
        response.put("orderCode", order.getOrderCode());
        response.put("transactionId", mockTransactionId);
        response.put("redirectUrl", frontendUrl + "/payment-result?success=true&orderCode=" + order.getOrderCode() + "&transactionId=" + mockTransactionId + "&message=Thanh%20toan%20thanh%20cong");

        return ResponseEntity.ok(ApiResponse.success(response, "Mock payment thành công"));
    }

    /**
     * Mock thanh toán thất bại cho ZaloPay
     */
    @PostMapping("/zalopay/failed")
    @Operation(summary = "Mock ZaloPay payment thất bại (DEV ONLY)")
    public ResponseEntity<ApiResponse<Map<String, String>>> mockZaloPayFailed(
            @RequestParam Long orderId) {
        
        log.warn("🚨 MOCK PAYMENT FAILED - DEV ONLY - Order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        order.setPaymentStatus(Order.PaymentStatus.failed);
        orderRepository.save(order);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock payment thất bại");
        response.put("orderCode", order.getOrderCode());
        response.put("redirectUrl", frontendUrl + "/payment-result?success=false&orderCode=" + order.getOrderCode() + "&message=Thanh%20toan%20that%20bai");

        return ResponseEntity.ok(ApiResponse.success(response, "Mock payment thất bại"));
    }
}
