package com.kinhduanpc.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {
    private Long id;
    private String role;
    private String status;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    // Thống kê cho khách hàng
    private Integer orderCount;
    private BigDecimal totalSpent;
}
