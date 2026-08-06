package com.kinhduanpc.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
