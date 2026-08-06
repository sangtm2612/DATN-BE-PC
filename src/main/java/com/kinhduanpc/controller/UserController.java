package com.kinhduanpc.controller;

import com.kinhduanpc.dto.*;
import com.kinhduanpc.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Quản lý người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMe(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            Authentication auth, 
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            userService.updateProfile(userId, request)));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<UserAddressDTO>>> getAddresses(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            userService.getUserAddresses(userId)));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<UserAddressDTO>> addAddress(
            Authentication auth, 
            @Valid @RequestBody UserAddressRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            userService.addAddress(userId, request)));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<UserAddressDTO>> updateAddress(
            Authentication auth, 
            @PathVariable Long id, 
            @Valid @RequestBody UserAddressRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            userService.updateAddress(userId, id, request)));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            Authentication auth, 
            @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        userService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa địa chỉ"));
    }

    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            Authentication auth, 
            @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        userService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đặt địa chỉ mặc định"));
    }
}
