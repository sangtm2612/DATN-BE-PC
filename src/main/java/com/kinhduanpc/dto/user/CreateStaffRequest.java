package com.kinhduanpc.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStaffRequest {

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    private String phone;

    @NotBlank @Size(min = 6)
    private String password;

    // staff | technician
    @NotBlank
    private String role;
}
