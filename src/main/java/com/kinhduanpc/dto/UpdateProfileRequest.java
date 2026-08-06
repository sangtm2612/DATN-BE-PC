package com.kinhduanpc.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
}
