package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.UserAddress;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserAddressRepository;
import com.kinhduanpc.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
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

    private final UserRepository userRepo;
    private final UserAddressRepository addressRepo;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMe(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            userRepo.findById(userId).orElseThrow(() -> AppException.notFound("Người dùng"))));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            Authentication auth, @RequestBody UpdateProfileRequest req) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("Người dùng"));
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone()    != null) user.setPhone(req.getPhone());
        if (req.getGender()   != null) user.setGender(req.getGender());
        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        return ResponseEntity.ok(ApiResponse.success(userRepo.save(user)));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<UserAddress>>> getAddresses(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(addressRepo.findByUserIdOrderByIsDefaultDesc(userId)));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<UserAddress>> addAddress(
            Authentication auth, @RequestBody UserAddress req) {
        Long userId = (Long) auth.getPrincipal();
        if (addressRepo.countByUserId(userId) >= 5) {
            throw AppException.badRequest("MAX_ADDRESSES", "Tối đa 5 địa chỉ giao hàng");
        }
        User user = userRepo.findById(userId).orElseThrow();
        req.setUser(user);
        if (req.getIsDefault()) addressRepo.clearDefault(userId);
        return ResponseEntity.ok(ApiResponse.success(addressRepo.save(req)));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<UserAddress>> updateAddress(
            Authentication auth, @PathVariable Long id, @RequestBody UserAddress req) {
        Long userId = (Long) auth.getPrincipal();
        UserAddress addr = addressRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Địa chỉ"));
        if (!addr.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Không có quyền");
        }
        addr.setFullName(req.getFullName()); addr.setPhone(req.getPhone());
        addr.setProvince(req.getProvince()); addr.setDistrict(req.getDistrict());
        addr.setWard(req.getWard()); addr.setAddressDetail(req.getAddressDetail());
        if (req.getIsDefault()) { addressRepo.clearDefault(userId); addr.setIsDefault(true); }
        return ResponseEntity.ok(ApiResponse.success(addressRepo.save(addr)));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        UserAddress addr = addressRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Địa chỉ"));
        if (!addr.getUser().getId().equals(userId)) throw AppException.forbidden("Không có quyền");
        if (addr.getIsDefault()) throw AppException.badRequest("CANNOT_DELETE_DEFAULT", "Không thể xóa địa chỉ mặc định");
        addressRepo.delete(addr);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa địa chỉ"));
    }

    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        addressRepo.clearDefault(userId);
        UserAddress addr = addressRepo.findById(id).orElseThrow();
        addr.setIsDefault(true);
        addressRepo.save(addr);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đặt địa chỉ mặc định"));
    }

    @Data
    static class UpdateProfileRequest {
        private String fullName;
        private String phone;
        private String gender;
        private java.time.LocalDate dateOfBirth;
    }
}
