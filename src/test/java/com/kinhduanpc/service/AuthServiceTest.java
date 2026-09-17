package com.kinhduanpc.service;

import com.kinhduanpc.dto.auth.LoginRequest;
import com.kinhduanpc.dto.auth.RegisterRequest;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.UserTokenRepository;
import com.kinhduanpc.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — covers UT_USER_01, UT_USER_02, UT_USER_03
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private UserTokenRepository tokenRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private VoucherPolicyService voucherPolicyService;

    @InjectMocks
    private AuthService authService;

    // ──────────────────────────────────────────────────────────────────────
    // UT_USER_01: Đăng ký với email hợp lệ → 201 Created (user được tạo)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_USER_01 – Đăng ký với email hợp lệ → user được lưu vào DB")
    void register_withValidEmail_savesUser() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nguyễn Văn A");
        req.setEmail("test@test.com");
        req.setPhone("0901234567");
        req.setPassword("Password1");

        when(userRepo.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepo.existsByPhone("0901234567")).thenReturn(false);

        assertDoesNotThrow(() -> authService.register(req));
        verify(userRepo, times(1)).save(any(User.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_USER_02: Đăng ký email đã tồn tại → 409 Conflict
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_USER_02 – Đăng ký email đã tồn tại → 409 Conflict EMAIL_EXISTS")
    void register_withExistingEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Nguyễn Văn A");
        req.setEmail("existing@test.com");
        req.setPhone("0901234567");
        req.setPassword("Password1");

        when(userRepo.existsByEmail("existing@test.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("EMAIL_EXISTS", ex.getErrorCode());
        verify(userRepo, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_USER_03: Đăng nhập sai mật khẩu → 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_USER_03 – Đăng nhập sai mật khẩu → 401 Unauthorized")
    void login_withWrongPassword_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setCredential("test@test.com");
        req.setPassword("wrongpassword");

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .build();

        when(userRepo.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }
}
