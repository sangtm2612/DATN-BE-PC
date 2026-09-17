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

    // Demo: redirect tất cả email đến địa chỉ này thay vì email thật của user
    private static final String DEMO_REDIRECT_EMAIL = "sangtm004.student@ehou.edu.vn";

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
    public void sendReturnRequestUpdate(String to, String name, String returnCode,
            String decision, String resolution, String refundAmount, String staffNote) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(DEMO_REDIRECT_EMAIL);
            boolean approved = "approved".equals(decision);
            boolean rejected = "rejected".equals(decision);
            boolean completed = "completed".equals(decision);
            msg.setSubject((approved ? "Yêu cầu đổi/trả được duyệt" :
                            rejected ? "Yêu cầu đổi/trả không được chấp nhận" :
                            "Yêu cầu đổi/trả đã hoàn tất") + " - " + returnCode);
            StringBuilder sb = new StringBuilder("Xin chào ").append(name).append(",\n\n");
            sb.append("Yêu cầu đổi/trả ").append(returnCode).append(" của bạn: ");
            if (approved) {
                sb.append("ĐÃ ĐƯỢC DUYỆT.\n");
                if ("refund".equals(resolution)) sb.append("Hình thức xử lý: Hoàn tiền").append(refundAmount != null ? " " + refundAmount : "").append("\n");
                else if ("exchange".equals(resolution)) sb.append("Hình thức xử lý: Đổi hàng mới\n");
            } else if (rejected) {
                sb.append("KHÔNG ĐƯỢC CHẤP NHẬN.\n");
            } else {
                sb.append("ĐÃ HOÀN TẤT.\n");
            }
            if (staffNote != null && !staffNote.isBlank()) sb.append("Ghi chú từ nhân viên: ").append(staffNote).append("\n");
            sb.append("\nXem chi tiết tại: ").append(frontendUrl).append("/account/returns");
            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send return request update email: {}", e.getMessage());
        }
    }

    @Async
    public void sendServiceRequestUpdate(String to, String name, String serviceCode,
            String status, String diagnosis) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(DEMO_REDIRECT_EMAIL);
            String statusLabel = switch (status) {
                case "received"      -> "Đã tiếp nhận";
                case "diagnosing"    -> "Đang chẩn đoán";
                case "repairing"     -> "Đang sửa chữa";
                case "waiting_part"  -> "Chờ linh kiện";
                case "done"          -> "Sửa chữa hoàn tất";
                case "returned"      -> "Đã trả máy";
                default -> status;
            };
            msg.setSubject("[KinhDuanPC] Yêu cầu sửa chữa " + serviceCode + " — " + statusLabel);
            StringBuilder sb = new StringBuilder("Xin chào ").append(name).append(",\n\n");
            sb.append("Yêu cầu sửa chữa ").append(serviceCode).append(" vừa được cập nhật trạng thái: ")
              .append(statusLabel).append("\n");
            if (diagnosis != null && !diagnosis.isBlank())
                sb.append("\nKết quả chẩn đoán: ").append(diagnosis).append("\n");
            if ("done".equals(status))
                sb.append("\nThiết bị của bạn đã được sửa xong. Vui lòng liên hệ cửa hàng để sắp xếp nhận máy.\n");
            if ("returned".equals(status))
                sb.append("\nThiết bị đã được bàn giao lại cho bạn. Cảm ơn bạn đã tin tưởng KinhDuanPC!\n");
            sb.append("\nXem chi tiết tại: ").append(frontendUrl).append("/account/warranties");
            sb.append("\n\nTrân trọng,\nĐội ngũ KinhDuanPC");
            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send service request update email: {}", e.getMessage());
        }
    }

    @Async
    public void sendRepairCostQuote(String to, String name, String serviceCode,
            String productName, String diagnosis, String repairCost) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(DEMO_REDIRECT_EMAIL);
            msg.setSubject("[KinhDuanPC] Báo giá sửa chữa " + serviceCode + " — " + repairCost + " · Cần xác nhận");
            StringBuilder sb = new StringBuilder("Xin chào ").append(name).append(",\n\n");
            sb.append("Kỹ thuật viên đã hoàn thành chẩn đoán thiết bị của bạn và gửi báo giá sửa chữa:\n\n");
            sb.append("  Mã yêu cầu : ").append(serviceCode).append("\n");
            sb.append("  Sản phẩm   : ").append(productName).append("\n");
            if (diagnosis != null && !diagnosis.isBlank())
                sb.append("  Chẩn đoán  : ").append(diagnosis).append("\n");
            sb.append("  Chi phí    : ").append(repairCost).append("\n\n");
            sb.append("Vui lòng đăng nhập và xác nhận hoặc từ chối báo giá này:\n");
            sb.append(frontendUrl).append("/account/warranties\n\n");
            sb.append("Lưu ý: Nếu bạn không xác nhận trong vòng 7 ngày, yêu cầu có thể bị hủy.\n");
            sb.append("\nTrân trọng,\nĐội ngũ KinhDuanPC");
            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send repair cost quote email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdate(String to, String name, String orderCode, String status, String staffNote) {
        try {
            String statusLabel = switch (status) {
                case "pending"         -> "Chờ xác nhận";
                case "pending_deposit" -> "Chờ đặt cọc";
                case "confirmed"       -> "Đã xác nhận";
                case "processing"      -> "Đang chuẩn bị hàng";
                case "shipping"        -> "Đang giao hàng";
                case "delivered"       -> "Đã giao hàng";
                case "completed"       -> "Hoàn tất";
                case "cancelled"       -> "Đã hủy";
                case "refunded"        -> "Đã hoàn tiền";
                default                -> status;
            };
            String statusDetail = switch (status) {
                case "confirmed"  -> "Đơn hàng của bạn đã được xác nhận và sẽ sớm được xử lý.";
                case "processing" -> "Chúng tôi đang chuẩn bị hàng hóa cho đơn hàng của bạn.";
                case "shipping"   -> "Đơn hàng của bạn đang trên đường giao đến bạn. Vui lòng chú ý điện thoại để nhận hàng.";
                case "delivered"  -> "Đơn hàng đã được giao thành công. Nếu có bất kỳ vấn đề gì, vui lòng liên hệ chúng tôi trong 7 ngày.";
                case "completed"  -> "Giao dịch đã hoàn tất. Cảm ơn bạn đã tin tưởng mua sắm tại KinhDuanPC!";
                case "cancelled"  -> "Đơn hàng của bạn đã bị hủy. Nếu bạn đã thanh toán, chúng tôi sẽ hoàn tiền trong 3–5 ngày làm việc.";
                case "refunded"   -> "Tiền hoàn trả đã được xử lý và sẽ về tài khoản của bạn trong 3–5 ngày làm việc.";
                default           -> "";
            };

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(DEMO_REDIRECT_EMAIL);
            msg.setSubject("[KinhDuanPC] Đơn hàng " + orderCode + " — " + statusLabel);

            StringBuilder sb = new StringBuilder();
            sb.append("Xin chào ").append(name != null ? name : "Quý khách").append(",\n\n");
            sb.append("Đơn hàng ").append(orderCode).append(" của bạn vừa được cập nhật trạng thái:\n");
            sb.append("► ").append(statusLabel).append("\n\n");
            if (!statusDetail.isEmpty()) sb.append(statusDetail).append("\n\n");
            if (staffNote != null && !staffNote.isBlank()) {
                sb.append("Ghi chú từ nhân viên: ").append(staffNote).append("\n\n");
            }
            sb.append("Xem chi tiết đơn hàng tại: ")
              .append(frontendUrl).append("/account/orders/").append(orderCode).append("\n\n");
            sb.append("Trân trọng,\nĐội ngũ KinhDuanPC");

            msg.setText(sb.toString());
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send status update email: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(DEMO_REDIRECT_EMAIL);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
