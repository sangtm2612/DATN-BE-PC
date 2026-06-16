package com.kinhduanpc.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

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
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("orderCode", orderCode);
            ctx.setVariable("totalAmount", totalAmount);
            ctx.setVariable("orderLink", frontendUrl + "/account/orders/" + orderCode);
            String html = templateEngine.process("email/order-confirm", ctx);
            sendHtmlEmail(to, "Xác nhận đơn hàng " + orderCode + " - KinhDuanPC", html);
        } catch (Exception e) {
            log.error("Failed to send order confirmation to {}: {}", to, e.getMessage());
        }
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
