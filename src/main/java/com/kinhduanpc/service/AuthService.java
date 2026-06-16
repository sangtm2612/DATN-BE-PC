package com.kinhduanpc.service;

import com.kinhduanpc.dto.auth.*;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.UserToken;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.UserTokenRepository;
import com.kinhduanpc.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final UserTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public void register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw AppException.conflict("EMAIL_EXISTS", "Email đã được sử dụng");
        }
        if (userRepo.existsByPhone(req.getPhone())) {
            throw AppException.conflict("PHONE_EXISTS", "Số điện thoại đã được sử dụng");
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(User.UserRole.customer)
            .status(User.UserStatus.active)
            .emailVerified(false)
            .build();

        userRepo.save(user);

        // Tạo OTP xác thực email
        String otp = generateOtp();
        UserToken token = UserToken.builder()
            .user(user)
            .token(otp)
            .tokenType("email_verify")
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
        tokenRepo.save(token);

        emailService.sendVerificationOtp(user.getEmail(), user.getFullName(), otp);
        log.info("Registered user: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmailOrPhone(req.getCredential())
            .orElseThrow(() -> AppException.unauthorized("Email/SĐT hoặc mật khẩu không đúng"));

        // Check account lock
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            throw AppException.unauthorized("Tài khoản tạm thời bị khóa. Vui lòng thử lại sau " +
                user.getLockedUntil().toString());
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            userRepo.incrementLoginAttempts(user.getId());
            if (user.getLoginAttempts() + 1 >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                userRepo.save(user);
                throw AppException.unauthorized("Sai mật khẩu quá 5 lần. Tài khoản bị khóa 30 phút");
            }
            throw AppException.unauthorized("Email/SĐT hoặc mật khẩu không đúng");
        }

        if (user.getStatus() == User.UserStatus.banned) {
            throw AppException.unauthorized("Tài khoản bị khóa vĩnh viễn. Vui lòng liên hệ CSKH");
        }

        userRepo.resetLoginAttempts(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        return buildAuthResponse(user);
    }

    public void verifyEmail(String otp) {
        UserToken token = tokenRepo.findByToken(otp)
            .orElseThrow(() -> AppException.badRequest("INVALID_OTP", "Mã OTP không hợp lệ"));

        if (token.isExpired()) throw AppException.badRequest("OTP_EXPIRED", "Mã OTP đã hết hạn");
        if (token.isUsed()) throw AppException.badRequest("OTP_USED", "Mã OTP đã được sử dụng");

        token.setUsedAt(LocalDateTime.now());
        tokenRepo.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        UserToken token = tokenRepo.findByToken(refreshToken)
            .orElseThrow(() -> AppException.unauthorized("Refresh token không hợp lệ"));

        if (token.isExpired() || token.isUsed()) {
            throw AppException.unauthorized("Refresh token đã hết hạn");
        }

        return buildAuthResponse(token.getUser());
    }

    public void forgotPassword(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            tokenRepo.deleteByUserIdAndType(user.getId(), "password_reset");
            String resetToken = UUID.randomUUID().toString();
            UserToken token = UserToken.builder()
                .user(user)
                .token(resetToken)
                .tokenType("password_reset")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
            tokenRepo.save(token);
            emailService.sendPasswordReset(email, user.getFullName(), resetToken);
        });
    }

    public void resetPassword(String resetToken, String newPassword) {
        UserToken token = tokenRepo.findByToken(resetToken)
            .orElseThrow(() -> AppException.badRequest("INVALID_TOKEN", "Link đặt lại mật khẩu không hợp lệ"));

        if (token.isExpired()) throw AppException.badRequest("TOKEN_EXPIRED", "Link đặt lại mật khẩu đã hết hạn");

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepo.save(token);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Lưu refresh token
        tokenRepo.deleteByUserIdAndType(user.getId(), "refresh");
        UserToken rt = UserToken.builder()
            .user(user)
            .token(refreshToken)
            .tokenType("refresh")
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();
        tokenRepo.save(rt);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.getEmailVerified())
                .build())
            .build();
    }

    private String generateOtp() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
