package com.kinhduanpc.service;

import com.kinhduanpc.config.VNPayConfig;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.Payment;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PaymentRepository;
import com.kinhduanpc.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(Long orderId, String ipAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        // Validate order
        if (!order.getPaymentMethod().equals(Order.PaymentMethod.vnpay)) {
            throw AppException.badRequest("INVALID_PAYMENT_METHOD", "Đơn hàng không sử dụng VNPay");
        }
        if (!order.getPaymentStatus().equals(Order.PaymentStatus.pending)) {
            throw AppException.badRequest("INVALID_PAYMENT_STATUS", "Đơn hàng đã thanh toán hoặc đã hủy");
        }

        // Build VNPay parameters
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(order.getTotalAmount().longValue() * 100)); // VNPay yêu cầu số tiền * 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderCode()); // Mã đơn hàng làm mã giao dịch
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Timestamp
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Expire after 15 minutes
        calendar.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Build query string
        String queryString = VNPayUtil.buildQueryString(vnpParams);

        // Generate secure hash
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        // Build final URL
        String paymentUrl = vnPayConfig.getVnpUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        log.info("Created VNPay payment URL for order: {}", order.getOrderCode());
        return paymentUrl;
    }

    /**
     * Xử lý callback từ VNPay (IPN - Instant Payment Notification)
     */
    @Transactional
    public Map<String, Object> handleCallback(Map<String, String> vnpParams) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Verify secure hash
            String vnpSecureHash = vnpParams.get("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            String signValue = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), VNPayUtil.buildQueryString(vnpParams));

            if (!signValue.equals(vnpSecureHash)) {
                log.error("Invalid VNPay signature");
                result.put("RspCode", "97");
                result.put("Message", "Invalid Signature");
                return result;
            }

            // 2. Get order info
            String orderCode = vnpParams.get("vnp_TxnRef");
            String transactionId = vnpParams.get("vnp_TransactionNo");
            String responseCode = vnpParams.get("vnp_ResponseCode");
            long amount = Long.parseLong(vnpParams.get("vnp_Amount")) / 100;

            Order order = orderRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

            // 3. Check amount
            if (order.getTotalAmount().longValue() != amount) {
                log.error("Invalid amount for order: {}", orderCode);
                result.put("RspCode", "04");
                result.put("Message", "Invalid Amount");
                return result;
            }

            // 4. Check if payment already processed
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(transactionId);
            if (existingPayment.isPresent()) {
                log.info("Payment already processed: {}", transactionId);
                result.put("RspCode", "02");
                result.put("Message", "Order Already Confirmed");
                return result;
            }

            // 5. Process payment based on response code
            if ("00".equals(responseCode)) {
                // Payment success
                order.setPaymentStatus(Order.PaymentStatus.paid);

                // Create payment record
                Payment payment = Payment.builder()
                        .order(order)
                        .transactionId(transactionId)
                        .gateway("vnpay")
                        .amount(order.getTotalAmount())
                        .currency("VND")
                        .status(Order.PaymentStatus.paid)
                        .gatewayResponse(vnpParams.toString())
                        .paidAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(payment);
                orderRepository.save(order);

                log.info("Payment successful for order: {}", orderCode);
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            } else {
                // Payment failed
                order.setPaymentStatus(Order.PaymentStatus.failed);
                orderRepository.save(order);

                log.warn("Payment failed for order: {}, response code: {}", orderCode, responseCode);
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            }

        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }

        return result;
    }

    /**
     * Xử lý return URL (redirect từ VNPay về frontend)
     */
    public Map<String, Object> handleReturn(Map<String, String> vnpParams) {
        Map<String, Object> result = new HashMap<>();

        // Verify secure hash
        String vnpSecureHash = vnpParams.get("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String signValue = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), VNPayUtil.buildQueryString(vnpParams));

        if (!signValue.equals(vnpSecureHash)) {
            result.put("success", false);
            result.put("message", "Chữ ký không hợp lệ");
            return result;
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        String orderCode = vnpParams.get("vnp_TxnRef");
        String transactionId = vnpParams.get("vnp_TransactionNo");

        if ("00".equals(responseCode)) {
            result.put("success", true);
            result.put("message", "Thanh toán thành công");
            result.put("orderCode", orderCode);
            result.put("transactionId", transactionId);
        } else {
            result.put("success", false);
            result.put("message", getResponseMessage(responseCode));
            result.put("orderCode", orderCode);
        }

        return result;
    }

    /**
     * Get message từ VNPay response code
     */
    private String getResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65" -> "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch thất bại";
        };
    }
}
