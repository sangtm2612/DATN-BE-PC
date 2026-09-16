package com.kinhduanpc.service;

import com.kinhduanpc.entity.Order;
import com.kinhduanpc.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OrderRepository orderRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendVerificationOtp(String to, String name, String otp) {
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("otp", otp);
            String html = templateEngine.process("email/verify-otp", ctx);
            sendHtmlEmail(to, "Xác thực tài khoản KinhDuanPC", html);
        } catch (Exception e) {
            log.error("Failed to send verification OTP to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordReset(String to, String name, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("resetLink", resetLink);
            String html = templateEngine.process("email/reset-password", ctx);
            sendHtmlEmail(to, "Đặt lại mật khẩu KinhDuanPC", html);
        } catch (Exception e) {
            log.error("Failed to send password reset to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmation(String to, String name, String orderCode, String totalAmount) {
        sendOrderConfirmation(to, name, orderCode, totalAmount, null, null, null, null, null, null, null, null);
    }

    @Async
    public void sendOrderConfirmation(String to, String name, String orderCode, String totalAmount, String phone) {
        sendOrderConfirmation(to, name, orderCode, totalAmount, phone, null, null, null, null, null, null, null);
    }

    @Async
    public void sendOrderConfirmation(String to, String name, String orderCode, String totalAmount,
                                      String phone, String shippingPhone, java.util.List<OrderItemDto> items,
                                      String subtotal, String shippingFee, String discount,
                                      String shippingAddress, String paymentMethod) {
        sendOrderConfirmation(to, name, orderCode, totalAmount, phone, shippingPhone, items,
            subtotal, shippingFee, discount, shippingAddress, paymentMethod, false, null, null);
    }

    @Async
    public void sendOrderConfirmation(String to, String name, String orderCode, String totalAmount,
                                      String phone, String shippingPhone, java.util.List<OrderItemDto> items,
                                      String subtotal, String shippingFee, String discount,
                                      String shippingAddress, String paymentMethod,
                                      boolean isCodDeposit, String depositAmount, String remainingAmount) {
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("orderCode", orderCode);
            ctx.setVariable("totalAmount", totalAmount);
            ctx.setVariable("shippingPhone", shippingPhone);
            ctx.setVariable("items", items);
            ctx.setVariable("subtotal", subtotal);
            ctx.setVariable("shippingFee", shippingFee);
            ctx.setVariable("discount", discount);
            ctx.setVariable("shippingAddress", shippingAddress);
            ctx.setVariable("paymentMethod", paymentMethod);
            ctx.setVariable("isCodDeposit", isCodDeposit);
            ctx.setVariable("depositAmount", depositAmount);
            ctx.setVariable("remainingAmount", remainingAmount);

            String orderLink;
            if (isCodDeposit && shippingPhone != null) {
                // Nút "Thanh toán cọc ngay" → trang thanh toán cọc
                orderLink = frontendUrl + "/thanh-toan-coc?orderCode=" + orderCode
                    + "&phone=" + shippingPhone;
            } else if (phone != null) {
                orderLink = frontendUrl + "/tra-don-hang?code=" + orderCode + "&phone=" + phone;
            } else {
                orderLink = frontendUrl + "/account/orders";
            }
            ctx.setVariable("orderLink", orderLink);
            ctx.setVariable("isGuest", phone != null);

            String subject = isCodDeposit
                ? "Yêu cầu thanh toán cọc đơn hàng " + orderCode + " - KinhDuanPC"
                : "Xác nhận đơn hàng " + orderCode + " - KinhDuanPC";
            String html = templateEngine.process("email/order-confirm", ctx);
            sendHtmlEmail(to, subject, html);
        } catch (Exception e) {
            log.error("Failed to send order confirmation to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendDepositConfirmed(String to, String name, String orderCode,
                                     String depositAmount, String remainingAmount, String orderLink) {
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("orderCode", orderCode);
            ctx.setVariable("depositAmount", depositAmount);
            ctx.setVariable("remainingAmount", remainingAmount);
            ctx.setVariable("orderLink", orderLink);
            String html = templateEngine.process("email/deposit-confirmed", ctx);
            sendHtmlEmail(to, "Đặt cọc thành công - Đơn hàng " + orderCode + " đã được xác nhận!", html);
        } catch (Exception e) {
            log.error("Failed to send deposit confirmed email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Gửi email xác nhận đơn hàng sau khi thanh toán online thành công (VNPay/ZaloPay).
     * Nhận orderId để tự fetch order trong transaction mới — tránh LazyInitializationException.
     */
    @Async
    @Transactional(readOnly = true)
    public void sendOrderConfirmationFromOrder(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return;

            String recipientEmail = order.getUser() != null
                ? order.getUser().getEmail()
                : order.getGuestEmail();
            if (recipientEmail == null) return;

            String recipientName = order.getUser() != null
                ? order.getUser().getFullName()
                : order.getShippingName();
            String recipientPhone = order.getUser() != null
                ? order.getUser().getPhone()
                : order.getShippingPhone();

            List<OrderItemDto> emailItems = order.getItems().stream()
                .map(item -> new OrderItemDto(
                    item.getProductName(),
                    item.getQuantity(),
                    String.format("%,.0fđ", item.getTotalPrice().doubleValue())
                ))
                .collect(Collectors.toList());

            String fullAddress = String.format("%s - %s\n%s, %s, %s",
                order.getShippingName(), order.getShippingPhone(),
                order.getShippingAddress(), order.getShippingWard(), order.getShippingProvince());

            String paymentMethodLabel = switch (order.getPaymentMethod()) {
                case vnpay -> "VNPay";
                case zalopay -> "ZaloPay";
                case momo -> "MoMo";
                case cod -> "COD";
                default -> order.getPaymentMethod().name();
            };

            sendOrderConfirmation(
                recipientEmail, recipientName, order.getOrderCode(),
                String.format("%,.0fđ", order.getTotalAmount().doubleValue()),
                recipientPhone, order.getShippingPhone(), emailItems,
                String.format("%,.0fđ", order.getSubtotal().doubleValue()),
                String.format("%,.0fđ", order.getShippingFee().doubleValue()),
                String.format("%,.0fđ", order.getDiscountAmount().doubleValue()),
                fullAddress, paymentMethodLabel,
                false, null, null
            );
        } catch (Exception e) {
            log.error("Failed to send post-payment confirmation for order {}: {}", orderId, e.getMessage());
        }
    }

    // DTO for email template
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderItemDto {
        private String productName;
        private Integer quantity;
        private String price;
    }

    @Async
    public void sendOrderStatusUpdate(String to, String name, String orderCode, String status) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject("Cập nhật đơn hàng " + orderCode);
            msg.setText("Xin chào " + name + ",\n\nĐơn hàng " + orderCode +
                " của bạn đã được cập nhật trạng thái: " + status +
                "\n\nXem chi tiết tại: " + frontendUrl + "/account/orders/" + orderCode);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send status update email: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
