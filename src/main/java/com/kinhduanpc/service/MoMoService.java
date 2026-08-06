package com.kinhduanpc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinhduanpc.config.MoMoConfig;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.Payment;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PaymentRepository;
import com.kinhduanpc.util.MoMoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {

    private final MoMoConfig momoConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo payment request tới MoMo
     */
    public String createPaymentUrl(Long orderId, String ipAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        // Validate order
        if (!order.getPaymentMethod().equals(Order.PaymentMethod.momo)) {
            throw AppException.badRequest("INVALID_PAYMENT_METHOD", "Đơn hàng không sử dụng MoMo");
        }
        if (!order.getPaymentStatus().equals(Order.PaymentStatus.pending)) {
            throw AppException.badRequest("INVALID_PAYMENT_STATUS", "Đơn hàng đã thanh toán hoặc đã hủy");
        }

        try {
            // Generate request ID
            String requestId = UUID.randomUUID().toString();
            String orderCode = order.getOrderCode();
            String orderInfo = "Thanh toan don hang " + order.getOrderCode();
            long amount = order.getTotalAmount().longValue();

            // Build raw signature
            String rawSignature = String.format(
                    "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    momoConfig.getAccessKey(),
                    amount,
                    "", // extraData
                    momoConfig.getNotifyUrl(),
                    orderCode,
                    orderInfo,
                    momoConfig.getPartnerCode(),
                    momoConfig.getReturnUrl(),
                    requestId,
                    momoConfig.getRequestType()
            );

            // Generate signature
            String signature = MoMoUtil.hmacSHA256(momoConfig.getSecretKey(), rawSignature);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", momoConfig.getPartnerCode());
            requestBody.put("partnerName", "KinhDuanPC");
            requestBody.put("storeId", "KinhDuanPCStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderCode);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", momoConfig.getReturnUrl());
            requestBody.put("ipnUrl", momoConfig.getNotifyUrl());
            requestBody.put("lang", "vi");
            requestBody.put("requestType", momoConfig.getRequestType());
            requestBody.put("autoCapture", true);
            requestBody.put("extraData", "");
            requestBody.put("signature", signature);

            // Send request to MoMo
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    momoConfig.getEndpoint(),
                    request,
                    String.class
            );

            // Parse response
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            Integer resultCode = (Integer) responseMap.get("resultCode");

            if (resultCode != null && resultCode == 0) {
                String payUrl = (String) responseMap.get("payUrl");
                log.info("Created MoMo payment URL for order: {}", order.getOrderCode());
                return payUrl;
            } else {
                String message = (String) responseMap.get("message");
                log.error("MoMo API error: {}", message);
                throw AppException.badRequest("MOMO_ERROR", message != null ? message : "Lỗi kết nối MoMo");
            }

        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            throw AppException.badRequest("MOMO_ERROR", "Không thể tạo thanh toán MoMo: " + e.getMessage());
        }
    }

    /**
     * Xử lý IPN callback từ MoMo
     */
    @Transactional
    public Map<String, Object> handleCallback(Map<String, Object> momoParams) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Verify signature
            String receivedSignature = (String) momoParams.get("signature");
            String partnerCode = (String) momoParams.get("partnerCode");
            String orderId = (String) momoParams.get("orderId");
            String requestId = (String) momoParams.get("requestId");
            Long amount = ((Number) momoParams.get("amount")).longValue();
            String orderInfo = (String) momoParams.get("orderInfo");
            String orderType = (String) momoParams.get("orderType");
            String transId = momoParams.get("transId") != null ? momoParams.get("transId").toString() : "";
            Integer resultCode = (Integer) momoParams.get("resultCode");
            String message = (String) momoParams.get("message");
            String payType = (String) momoParams.get("payType");
            String responseTime = momoParams.get("responseTime") != null ? momoParams.get("responseTime").toString() : "";
            String extraData = (String) momoParams.get("extraData");

            // Build signature to verify
            String rawSignature = String.format(
                    "accessKey=%s&amount=%d&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%d&transId=%s",
                    momoConfig.getAccessKey(),
                    amount,
                    extraData != null ? extraData : "",
                    message,
                    orderId,
                    orderInfo,
                    orderType,
                    partnerCode,
                    payType,
                    requestId,
                    responseTime,
                    resultCode,
                    transId
            );

            String signature = MoMoUtil.hmacSHA256(momoConfig.getSecretKey(), rawSignature);

            if (!signature.equals(receivedSignature)) {
                log.error("Invalid MoMo signature");
                result.put("resultCode", 97);
                result.put("message", "Invalid Signature");
                return result;
            }

            // 2. Get order
            Order order = orderRepository.findByOrderCode(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            // 3. Check amount
            if (order.getTotalAmount().longValue() != amount) {
                log.error("Invalid amount for order: {}", orderId);
                result.put("resultCode", 4);
                result.put("message", "Invalid Amount");
                return result;
            }

            // 4. Check if payment already processed
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(transId);
            if (existingPayment.isPresent()) {
                log.info("Payment already processed: {}", transId);
                result.put("resultCode", 2);
                result.put("message", "Order Already Confirmed");
                return result;
            }

            // 5. Process payment based on result code
            if (resultCode == 0) {
                // Payment success
                order.setPaymentStatus(Order.PaymentStatus.paid);

                // Create payment record
                Payment payment = Payment.builder()
                        .order(order)
                        .transactionId(transId)
                        .gateway("momo")
                        .amount(order.getTotalAmount())
                        .currency("VND")
                        .status(Order.PaymentStatus.paid)
                        .gatewayResponse(momoParams.toString())
                        .paidAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(payment);
                orderRepository.save(order);

                log.info("MoMo payment successful for order: {}", orderId);
                result.put("resultCode", 0);
                result.put("message", "Confirm Success");
            } else {
                // Payment failed
                order.setPaymentStatus(Order.PaymentStatus.failed);
                orderRepository.save(order);

                log.warn("MoMo payment failed for order: {}, result code: {}", orderId, resultCode);
                result.put("resultCode", 0);
                result.put("message", "Confirm Success");
            }

        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            result.put("resultCode", 99);
            result.put("message", "Unknown error");
        }

        return result;
    }

    /**
     * Xử lý return URL từ MoMo
     */
    public Map<String, Object> handleReturn(Map<String, Object> momoParams) {
        Map<String, Object> result = new HashMap<>();

        try {
            Integer resultCode = (Integer) momoParams.get("resultCode");
            String orderId = (String) momoParams.get("orderId");
            String transId = momoParams.get("transId") != null ? momoParams.get("transId").toString() : "";
            String message = (String) momoParams.get("message");

            if (resultCode == 0) {
                result.put("success", true);
                result.put("message", "Thanh toán thành công");
                result.put("orderCode", orderId);
                result.put("transactionId", transId);
            } else {
                result.put("success", false);
                result.put("message", message != null ? message : "Giao dịch thất bại");
                result.put("orderCode", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing MoMo return", e);
            result.put("success", false);
            result.put("message", "Lỗi xử lý kết quả thanh toán");
        }

        return result;
    }
}
