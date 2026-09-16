package com.kinhduanpc.controller;

import com.kinhduanpc.dto.*;
import com.kinhduanpc.dto.user.AdminUserResponse;
import com.kinhduanpc.dto.user.CreateStaffRequest;
import com.kinhduanpc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ===== Admin endpoints =====

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Danh sách tài khoản (tìm kiếm, lọc theo role)")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAdminUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserResponse> result = userService.getAdminUsers(role, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Chi tiết tài khoản (kèm thống kê đơn hàng với khách hàng)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getAdminUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAdminUserDetail(id)));
    }

    @PostMapping("/admin/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Tạo tài khoản nhân viên (staff/technician)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(userService.createStaff(req), "Tạo tài khoản thành công"));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Cập nhật trạng thái tài khoản (active/inactive/banned)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUserStatus(id, status)));
    }

    @PatchMapping("/admin/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Đổi vai trò tài khoản")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(
            @PathVariable Long id,
            @RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUserRole(id, role)));
    }

}
