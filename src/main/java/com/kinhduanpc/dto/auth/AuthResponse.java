package com.kinhduanpc.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserInfo user;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String role;
        private String avatarUrl;
        private Boolean emailVerified;
    }
}
