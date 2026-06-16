package com.kinhduanpc.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String credential; // email or phone

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
