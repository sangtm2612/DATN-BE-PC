package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.auth.*;
import com.kinhduanpc.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, xác thực")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(null, "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản"));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Xác thực email bằng OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String otp) {
        authService.verifyEmail(otp);
        return ResponseEntity.ok(ApiResponse.success(null, "Xác thực email thành công"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(refreshToken)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(null,
            "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công"));
    }
}
