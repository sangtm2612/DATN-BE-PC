package com.kinhduanpc.service;

import com.kinhduanpc.dto.UpdateProfileRequest;
import com.kinhduanpc.dto.UserAddressDTO;
import com.kinhduanpc.dto.UserAddressRequest;
import com.kinhduanpc.dto.UserDTO;
import com.kinhduanpc.dto.user.AdminUserResponse;
import com.kinhduanpc.dto.user.CreateStaffRequest;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.UserAddress;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.UserAddressRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final UserAddressRepository addressRepo;
    private final OrderRepository orderRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        return toDTO(user);
    }

    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        User updated = userRepo.save(user);
        return toDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<UserAddressDTO> getUserAddresses(Long userId) {
        return addressRepo.findByUserIdOrderByIsDefaultDesc(userId)
            .stream()
            .map(this::toAddressDTO)
            .toList();
    }

    public UserAddressDTO addAddress(Long userId, UserAddressRequest request) {
        // Validate max addresses
        if (addressRepo.countByUserId(userId) >= 5) {
            throw AppException.badRequest("MAX_ADDRESSES", "Tối đa 5 địa chỉ giao hàng");
        }

        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));

        // Clear default if this is set as default
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepo.clearDefault(userId);
        }

        UserAddress address = UserAddress.builder()
            .user(user)
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .province(request.getProvince())
            .district(request.getDistrict())
            .ward(request.getWard())
            .addressDetail(request.getAddressDetail())
            .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
            .build();

        UserAddress saved = addressRepo.save(address);
        return toAddressDTO(saved);
    }

    public UserAddressDTO updateAddress(Long userId, Long addressId, UserAddressRequest request) {
        UserAddress address = addressRepo.findById(addressId)
            .orElseThrow(() -> AppException.notFound("Địa chỉ"));

        // Check ownership
        if (!address.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Không có quyền");
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setAddressDetail(request.getAddressDetail());

        // Clear default if this is set as default
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepo.clearDefault(userId);
            address.setIsDefault(true);
        }

        UserAddress updated = addressRepo.save(address);
        return toAddressDTO(updated);
    }

    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepo.findById(addressId)
            .orElseThrow(() -> AppException.notFound("Địa chỉ"));

        // Check ownership
        if (!address.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Không có quyền");
        }

        // Cannot delete default address
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            throw AppException.badRequest("CANNOT_DELETE_DEFAULT", 
                "Không thể xóa địa chỉ mặc định");
        }

        addressRepo.delete(address);
    }

    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = addressRepo.findById(addressId)
            .orElseThrow(() -> AppException.notFound("Địa chỉ"));

        // Check ownership
        if (!address.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Không có quyền");
        }

        addressRepo.clearDefault(userId);
        address.setIsDefault(true);
        addressRepo.save(address);
    }

    // ===== Admin methods =====

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAdminUsers(String role, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        User.UserRole userRole = (role != null && !role.isBlank()) ? User.UserRole.valueOf(role) : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<User> users;
        if (kw == null && userRole == null) {
            users = userRepo.findAllByOrderByCreatedAtDesc(pageable);
        } else if (kw == null) {
            users = userRepo.findAllByRoleOrderByCreatedAtDesc(userRole, pageable);
        } else if (userRole == null) {
            users = userRepo.searchUsers(kw, pageable);
        } else {
            users = userRepo.searchUsersByRole(userRole, kw, pageable);
        }
        return users.map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getAdminUserDetail(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        AdminUserResponse resp = toAdminResponse(user);
        if (user.getRole() == User.UserRole.customer) {
            resp.setOrderCount(orderRepo.countCompletedByUserId(userId));
            resp.setTotalSpent(orderRepo.sumTotalSpentByUserId(userId));
        }
        return resp;
    }

    public AdminUserResponse createStaff(CreateStaffRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw AppException.badRequest("EMAIL_TAKEN", "Email đã được sử dụng");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank() && userRepo.existsByPhone(req.getPhone())) {
            throw AppException.badRequest("PHONE_TAKEN", "Số điện thoại đã được sử dụng");
        }

        User.UserRole role;
        try {
            role = User.UserRole.valueOf(req.getRole());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("INVALID_ROLE", "Role không hợp lệ");
        }
        if (role == User.UserRole.customer || role == User.UserRole.admin) {
            throw AppException.badRequest("INVALID_ROLE", "Chỉ được tạo tài khoản staff hoặc technician");
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(role)
            .status(User.UserStatus.active)
            .emailVerified(true)
            .build();
        return toAdminResponse(userRepo.save(user));
    }

    public AdminUserResponse updateUserStatus(Long userId, String status) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        user.setStatus(User.UserStatus.valueOf(status));
        return toAdminResponse(userRepo.save(user));
    }

    public AdminUserResponse updateUserRole(Long userId, String role) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        User.UserRole newRole = User.UserRole.valueOf(role);
        if (newRole == User.UserRole.admin) {
            throw AppException.badRequest("FORBIDDEN", "Không thể cấp quyền admin qua API");
        }
        user.setRole(newRole);
        return toAdminResponse(userRepo.save(user));
    }

    // Mapping methods
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getEmail()) // Use email as username
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .gender(user.getGender())
            .dateOfBirth(user.getDateOfBirth())
            .role(user.getRole().name())
            .isActive(user.getStatus() == User.UserStatus.active)
            .createdAt(user.getCreatedAt())
            .build();
    }

    private AdminUserResponse toAdminResponse(User user) {
        return AdminUserResponse.builder()
            .id(user.getId())
            .role(user.getRole().name())
            .status(user.getStatus().name())
            .email(user.getEmail())
            .phone(user.getPhone())
            .fullName(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .dateOfBirth(user.getDateOfBirth())
            .gender(user.getGender())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }

    private UserAddressDTO toAddressDTO(UserAddress address) {
        return UserAddressDTO.builder()
            .id(address.getId())
            .fullName(address.getFullName())
            .phone(address.getPhone())
            .province(address.getProvince())
            .district(address.getDistrict())
            .ward(address.getWard())
            .addressDetail(address.getAddressDetail())
            .isDefault(address.getIsDefault())
            .build();
    }
}
