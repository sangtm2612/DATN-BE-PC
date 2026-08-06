package com.kinhduanpc.service;

import com.kinhduanpc.dto.UpdateProfileRequest;
import com.kinhduanpc.dto.UserAddressDTO;
import com.kinhduanpc.dto.UserAddressRequest;
import com.kinhduanpc.dto.UserDTO;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.UserAddress;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserAddressRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final UserAddressRepository addressRepo;

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
