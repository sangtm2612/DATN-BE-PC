package com.kinhduanpc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinhduanpc.config.ZaloPayConfig;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.Payment;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PaymentRepository;
import com.kinhduanpc.util.ZaloPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloPayService {

    private final ZaloPayConfig zaloPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo payment request tới ZaloPay
     * @param orderId ID đơn hàng
     * @param ipAddress IP address của khách hàng
     */
    public String createPaymentUrl(Long orderId, String ipAddress) {
        return createPaymentUrl(orderId, null, ipAddress);
    }

    /**
     * Tạo payment request tới ZaloPay với số tiền tùy chỉnh
     * @param orderId ID đơn hàng
     * @param customAmount Số tiền thanh toán (null = thanh toán toàn bộ)
     * @param ipAddress IP address của khách hàng
     */
    public String createPaymentUrl(Long orderId, Long customAmount, String ipAddress) {
        log.info("=== Creating ZaloPay Payment URL ===");
        log.info("Order ID: {}, Custom Amount: {}, IP: {}", orderId, customAmount, ipAddress);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        log.info("Found order: {}, amount: {}, payment_method: {}, payment_status: {}", 
                order.getOrderCode(), order.getTotalAmount(), order.getPaymentMethod(), order.getPaymentStatus());

        // Validate order - Cho phép cả COD và ZaloPay
        if (!order.getPaymentMethod().equals(Order.PaymentMethod.zalopay)
                && !order.getPaymentMethod().equals(Order.PaymentMethod.cod)) {
            throw AppException.badRequest("INVALID_PAYMENT_METHOD", "Đơn hàng không hỗ trợ thanh toán ZaloPay");
        }
        if (!order.getPaymentStatus().equals(Order.PaymentStatus.pending)) {
            throw AppException.badRequest("INVALID_PAYMENT_STATUS", "Đơn hàng đã thanh toán hoặc đã hủy");
        }

        try {
            // Generate app_trans_id: yyMMdd_xxxx
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            String appTransId = date + "_" + order.getId();

            // Xác định số tiền thanh toán
            long amount = (customAmount != null) ? customAmount : order.getTotalAmount().longValue();
            long appTime = ZaloPayUtil.getCurrentTimeMillis();

            log.info("Generated app_trans_id: {}, amount: {}, app_time: {}", appTransId, amount, appTime);

            // Embed data for callback — redirect qua BE return endpoint để normalize params + truyền phone
            Map<String, Object> embedData = new HashMap<>();
            embedData.put("redirecturl", zaloPayConfig.getReturnUrl());

            // Item format: [{"itemid":"item_id","itemname":"item_name","itemprice":price,"itemquantity":quantity}]
            String item = "[{\"itemid\":\"" + order.getId() + "\",\"itemname\":\"Don hang " + order.getOrderCode() + "\",\"itemprice\":" + amount + ",\"itemquantity\":1}]";

            // Build data for MAC
            String appUser = order.getUser() != null
                ? (order.getUser().getEmail() != null ? order.getUser().getEmail() : order.getUser().getPhone())
                : "guest_" + order.getOrderCode();

            String data = zaloPayConfig.getAppId() + "|"
                    + appTransId + "|"
                    + appUser + "|"
                    + amount + "|"
                    + appTime + "|"
                    + objectMapper.writeValueAsString(embedData) + "|"
                    + item;

            log.info("MAC data string: {}", data);

            // Generate MAC
            String mac = ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey1(), data);
            log.info("Generated MAC: {}", mac);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
            requestBody.put("app_trans_id", appTransId);
            requestBody.put("app_user", appUser);
            requestBody.put("app_time", appTime);
            requestBody.put("amount", amount);
            requestBody.put("item", item);
            requestBody.put("embed_data", objectMapper.writeValueAsString(embedData));
            requestBody.put("description", "Thanh toan don hang " + order.getOrderCode());
            requestBody.put("bank_code", ""); // Empty for ZaloPay wallet
            requestBody.put("callback_url", zaloPayConfig.getCallbackUrl());
            requestBody.put("mac", mac);

            log.info("Request body: {}", objectMapper.writeValueAsString(requestBody));
            log.info("Callback URL: {}", zaloPayConfig.getCallbackUrl());
            log.info("ZaloPay Endpoint: {}", zaloPayConfig.getEndpoint());

            // Send request to ZaloPay
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    zaloPayConfig.getEndpoint(),
                    request,
                    String.class
            );

            log.info("ZaloPay response status: {}", response.getStatusCode());
            log.info("ZaloPay response body: {}", response.getBody());

            // Parse response
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            Integer returnCode = (Integer) responseMap.get("return_code");

            if (returnCode != null && returnCode == 1) {
                String orderUrl = (String) responseMap.get("order_url");
                log.info("✅ Created ZaloPay payment URL successfully for order: {}", order.getOrderCode());
                log.info("Payment URL: {}", orderUrl);
                return orderUrl;
            } else {
                String returnMessage = (String) responseMap.get("return_message");
                log.error("❌ ZaloPay API error - return_code: {}, message: {}", returnCode, returnMessage);
                throw AppException.badRequest("ZALOPAY_ERROR", returnMessage != null ? returnMessage : "Lỗi kết nối ZaloPay");
            }

        } catch (Exception e) {
            log.error("❌ Error creating ZaloPay payment", e);
            throw AppException.badRequest("ZALOPAY_ERROR", "Không thể tạo thanh toán ZaloPay: " + e.getMessage());
        }
    }

    /**
     * Xử lý callback từ ZaloPay
     */
    @Transactional
    public Map<String, Object> handleCallback(Map<String, Object> zaloPayParams) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("=== ZaloPay Callback Received ===");
            log.info("Raw params: {}", zaloPayParams);

            // 1. Get data and mac
            String dataStr = (String) zaloPayParams.get("data");
            String receivedMac = (String) zaloPayParams.get("mac");

            log.info("Data string: {}", dataStr);
            log.info("Received MAC: {}", receivedMac);

            // 2. Verify MAC
            String mac = ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey2(), dataStr);
            log.info("Calculated MAC: {}", mac);
            
            if (!mac.equals(receivedMac)) {
                log.error("Invalid ZaloPay MAC - Expected: {}, Received: {}", mac, receivedMac);
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
                return result;
            }

            log.info("MAC verification successful");

            // 3. Parse data
            Map<String, Object> dataMap = objectMapper.readValue(dataStr, Map.class);
            String appTransId = (String) dataMap.get("app_trans_id");
            Long amount = ((Number) dataMap.get("amount")).longValue();
            String zpTransId = dataMap.get("zp_trans_id") != null ? dataMap.get("zp_trans_id").toString() : "";

            log.info("Parsed - app_trans_id: {}, amount: {}, zp_trans_id: {}", appTransId, amount, zpTransId);

            // 4. Extract order ID from app_trans_id (format: yyMMdd_orderId)
            String[] parts = appTransId.split("_");
            if (parts.length != 2) {
                log.error("Invalid app_trans_id format: {}", appTransId);
                result.put("return_code", 0); // Still return success to ZaloPay
                result.put("return_message", "success");
                return result;
            }

            Long orderId = Long.parseLong(parts[1]);
            log.info("Extracted Order ID: {}", orderId);

            // 5. Get order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            log.info("Found order: {}, current status: {}", order.getOrderCode(), order.getPaymentStatus());

            // 6. Check amount - Chấp nhận cả thanh toán toàn bộ và thanh toán cọc
            long orderTotalAmount = order.getTotalAmount().longValue();
            long orderDepositAmount = order.getDepositAmount().longValue();
            boolean isDepositPayment = (amount == orderDepositAmount && orderDepositAmount > 0);
            boolean isFullPayment = (amount == orderTotalAmount);
            
            if (!isDepositPayment && !isFullPayment) {
                log.error("Amount mismatch for order: {}. Expected {} (full) or {} (deposit), got {}", 
                    order.getOrderCode(), orderTotalAmount, orderDepositAmount, amount);
                result.put("return_code", 0); // Still return success to avoid retry
                result.put("return_message", "success");
                return result;
            }

            // 7. Check if payment already processed
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(zpTransId);
            if (existingPayment.isPresent()) {
                log.info("Payment already processed: {}", zpTransId);
                result.put("return_code", 1);
                result.put("return_message", "success");
                return result;
            }

            // 8. Process payment - ZaloPay callback only fires on success
            if (isDepositPayment) {
                // Thanh toán cọc thành công
                order.setDepositPaid(true);
                
                // Nếu order đang ở trạng thái pending_deposit, chuyển sang pending
                if (order.getStatus() == Order.OrderStatus.pending_deposit) {
                    order.setStatus(Order.OrderStatus.pending);
                    // Set auto-cancel sau 48h kể từ khi cọc thành công
                    order.setAutoCancelAt(LocalDateTime.now().plusHours(48));
                }
                
                log.info("Deposit payment successful for order: {} (amount: {}), status changed to pending", order.getOrderCode(), amount);
            } else {
                // Thanh toán toàn bộ thành công
                order.setPaymentStatus(Order.PaymentStatus.paid);
                log.info("Full payment successful for order: {} (amount: {})", order.getOrderCode(), amount);
            }

            // Create payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .transactionId(zpTransId)
                    .gateway("zalopay")
                    .amount(BigDecimal.valueOf(amount))
                    .currency("VND")
                    .status(Order.PaymentStatus.paid)
                    .gatewayResponse(dataStr)
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
            orderRepository.save(order);

            if (isDepositPayment) {
                sendDepositConfirmedEmail(order);
            } else {
                // Thanh toán toàn bộ: gửi email xác nhận đơn hàng
                emailService.sendOrderConfirmationFromOrder(order.getId());
            }

            log.info("✅ ZaloPay payment successful for order: {}", order.getOrderCode());
            result.put("return_code", 1);
            result.put("return_message", "success");

        } catch (Exception e) {
            log.error("❌ Error processing ZaloPay callback", e);
            result.put("return_code", 0); // Return 0 to avoid retry from ZaloPay
            result.put("return_message", "error");
        }

        return result;
    }

    private void sendDepositConfirmedEmail(Order order) {
        try {
            String recipientEmail = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();
            if (recipientEmail == null) return;

            String recipientName = order.getUser() != null ? order.getUser().getFullName() : order.getShippingName();
            String orderLink = frontendUrl + "/tra-don-hang?code=" + order.getOrderCode()
                + (order.getShippingPhone() != null ? "&phone=" + order.getShippingPhone() : "");
            String depositFormatted = String.format("%,.0fđ", order.getDepositAmount().doubleValue());
            String remainingFormatted = order.getRemainingAmount() != null
                ? String.format("%,.0fđ", order.getRemainingAmount().doubleValue()) : "0đ";
            emailService.sendDepositConfirmed(recipientEmail, recipientName, order.getOrderCode(),
                depositFormatted, remainingFormatted, orderLink);
        } catch (Exception e) {
            log.warn("Failed to send deposit confirmed email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    /**
     * Query order status from ZaloPay
     */
    public Map<String, Object> queryOrderStatus(String appTransId) {
        Map<String, Object> result = new HashMap<>();

        try {
            long appTime = ZaloPayUtil.getCurrentTimeMillis();

            // Build data for MAC
            String data = zaloPayConfig.getAppId() + "|" + appTransId + "|" + zaloPayConfig.getKey1();
            String mac = ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey1(), data);

            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
            requestBody.put("app_trans_id", appTransId);
            requestBody.put("mac", mac);

            // Send request
            String queryEndpoint = "https://sb-openapi.zalopay.vn/v2/query";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(queryEndpoint, request, String.class);
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            result = responseMap;

        } catch (Exception e) {
            log.error("Error querying ZaloPay order status", e);
            result.put("return_code", -1);
            result.put("return_message", "Query failed");
        }

        return result;
    }
}
