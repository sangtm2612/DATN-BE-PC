package com.kinhduanpc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinhduanpc.config.VNPayConfig;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.Payment;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PaymentRepository;
import com.kinhduanpc.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(Long orderId, String ipAddress) {
        return createPaymentUrl(orderId, null, ipAddress);
    }

    /**
     * Tạo URL thanh toán VNPay với số tiền tùy chỉnh
     */
    public String createPaymentUrl(Long orderId, Long customAmount, String ipAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng"));

        if (!order.getPaymentMethod().equals(Order.PaymentMethod.vnpay)
                && !order.getPaymentMethod().equals(Order.PaymentMethod.cod)) {
            throw AppException.badRequest("INVALID_PAYMENT_METHOD", "Đơn hàng không hỗ trợ thanh toán VNPay");
        }
        if (!order.getPaymentStatus().equals(Order.PaymentStatus.pending)) {
            throw AppException.badRequest("INVALID_PAYMENT_STATUS", "Đơn hàng đã thanh toán hoặc đã hủy");
        }

        long paymentAmount = (customAmount != null) ? customAmount : order.getTotalAmount().longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(paymentAmount * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(calendar.getTime()));
        calendar.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(calendar.getTime()));

        String queryString = VNPayUtil.buildQueryString(vnpParams);
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        log.info("Created VNPay payment URL for order: {}", order.getOrderCode());
        return vnPayConfig.getVnpUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xử lý callback từ VNPay (IPN)
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
            long orderTotalAmount = order.getTotalAmount().longValue();
            long orderDepositAmount = order.getDepositAmount().longValue();
            boolean isDepositPayment = (amount == orderDepositAmount && orderDepositAmount > 0);
            boolean isFullPayment = (amount == orderTotalAmount);

            if (!isDepositPayment && !isFullPayment) {
                log.error("Invalid amount for order: {}. Expected {} or {}, got {}",
                    orderCode, orderTotalAmount, orderDepositAmount, amount);
                result.put("RspCode", "04");
                result.put("Message", "Invalid Amount");
                return result;
            }

            // 4. Check if already processed
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(transactionId);
            if (existingPayment.isPresent()) {
                log.info("Payment already processed: {}", transactionId);
                result.put("RspCode", "02");
                result.put("Message", "Order Already Confirmed");
                return result;
            }

            // 5. Process payment
            if ("00".equals(responseCode)) {
                if (isDepositPayment) {
                    order.setDepositPaid(true);
                    if (order.getStatus() == Order.OrderStatus.pending_deposit) {
                        order.setStatus(Order.OrderStatus.pending);
                        order.setAutoCancelAt(LocalDateTime.now().plusHours(48));
                    }
                    log.info("Deposit payment successful for order: {} (amount: {})", orderCode, amount);
                } else {
                    order.setPaymentStatus(Order.PaymentStatus.paid);
                    log.info("Full payment successful for order: {} (amount: {})", orderCode, amount);
                }

                Payment payment = Payment.builder()
                        .order(order)
                        .transactionId(transactionId)
                        .gateway("vnpay")
                        .amount(BigDecimal.valueOf(amount))
                        .currency("VND")
                        .status(Order.PaymentStatus.paid)
                        .gatewayResponse(toJson(vnpParams))
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

                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            } else {
                if (isDepositPayment) {
                    log.warn("Deposit payment failed for order: {}, code: {}", orderCode, responseCode);
                } else {
                    order.setPaymentStatus(Order.PaymentStatus.failed);
                    log.warn("Payment failed for order: {}, code: {}", orderCode, responseCode);
                }
                orderRepository.save(order);

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

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
