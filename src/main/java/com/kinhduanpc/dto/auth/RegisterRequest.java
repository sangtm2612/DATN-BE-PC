package com.kinhduanpc.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 150, message = "Họ tên từ 2-150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mật khẩu tối thiểu 8 ký tự, có ít nhất 1 chữ hoa và 1 số")
    private String password;
}
