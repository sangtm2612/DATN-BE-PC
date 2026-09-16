package com.kinhduanpc.service;

import com.kinhduanpc.dto.VoucherDTO;
import com.kinhduanpc.dto.VoucherRequest;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.UserVoucher;
import com.kinhduanpc.entity.Voucher;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.UserVoucherRepository;
import com.kinhduanpc.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepo;
    private final UserVoucherRepository userVoucherRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<VoucherDTO> findAll() {
        return voucherRepo.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public VoucherDTO checkVoucher(String code) {
        return checkVoucher(code, null);
    }

    /**
     * Check voucher với user context để validate quyền sử dụng
     */
    @Transactional(readOnly = true)
    public VoucherDTO checkVoucher(String code, Long userId) {
        Voucher voucher = voucherRepo.findValidByCode(code, LocalDateTime.now())
            .orElseThrow(() -> AppException.badRequest("INVALID_VOUCHER", 
                "Voucher không hợp lệ hoặc đã hết hạn"));
        
        // Additional validation
        if (voucher.getUsageLimit() != null && 
            voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw AppException.badRequest("VOUCHER_EXHAUSTED", "Voucher đã hết lượt sử dụng");
        }

        // Check quyền sử dụng voucher PERSONAL
        if (voucher.getVoucherType() == Voucher.VoucherType.PERSONAL) {
            if (userId == null) {
                throw AppException.badRequest("VOUCHER_LOGIN_REQUIRED", 
                    "Voucher này yêu cầu đăng nhập để sử dụng");
            }
            
            UserVoucher userVoucher = userVoucherRepo.findByUserIdAndVoucherCode(userId, code)
                .orElseThrow(() -> AppException.forbidden(
                    "Bạn không có quyền sử dụng voucher này"));
            
            if (!userVoucher.isAvailable()) {
                throw AppException.badRequest("VOUCHER_NOT_AVAILABLE", 
                    "Voucher đã được sử dụng hoặc hết hạn");
            }
        }

        // Check usage per user limit (cho cả PUBLIC và PERSONAL)
        if (userId != null && voucher.getUsagePerUser() != null) {
            int usedCount = userVoucherRepo.countUsedByUserAndVoucher(userId, voucher.getId());
            if (usedCount >= voucher.getUsagePerUser()) {
                throw AppException.badRequest("VOUCHER_USER_LIMIT", 
                    "Bạn đã sử dụng hết số lần cho phép với voucher này");
            }
        }
        
        return toDTO(voucher);
    }

    /**
     * Phân phối voucher cho danh sách users
     */
    public void assignVoucherToUsers(Long voucherId, List<Long> userIds, 
                                      LocalDateTime expiresAt, Long assignedBy) {
        Voucher voucher = voucherRepo.findById(voucherId)
            .orElseThrow(() -> AppException.notFound("Voucher"));

        if (voucher.getVoucherType() != Voucher.VoucherType.PERSONAL) {
            throw AppException.badRequest("INVALID_VOUCHER_TYPE", 
                "Chỉ có thể phân phối voucher loại PERSONAL");
        }

        for (Long userId : userIds) {
            // Skip nếu user đã có voucher này rồi
            if (userVoucherRepo.existsByUserIdAndVoucherId(userId, voucherId)) {
                continue;
            }

            User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("User ID: " + userId));

            UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucher.Status.AVAILABLE)
                .assignedBy(assignedBy)
                .expiresAt(expiresAt)
                .build();

            userVoucherRepo.save(userVoucher);
        }
    }

    /**
     * Lấy danh sách voucher của user
     */
    @Transactional(readOnly = true)
    public List<VoucherDTO> getMyVouchers(Long userId, String status) {
        List<UserVoucher> userVouchers;
        
        if ("available".equalsIgnoreCase(status)) {
            userVouchers = userVoucherRepo.findAvailableByUserId(userId);
        } else {
            userVouchers = userVoucherRepo.findAllByUserId(userId);
            
            // Filter by status if provided
            if (status != null && !status.isBlank()) {
                UserVoucher.Status filterStatus = UserVoucher.Status.valueOf(status.toUpperCase());
                userVouchers = userVouchers.stream()
                    .filter(uv -> uv.getStatus() == filterStatus)
                    .toList();
            }
        }

        return userVouchers.stream()
            .map(uv -> {
                VoucherDTO dto = toDTO(uv.getVoucher());
                dto.setUserVoucherStatus(uv.getStatus().name());
                dto.setAssignedAt(uv.getAssignedAt());
                dto.setUsedAt(uv.getUsedAt());
                dto.setUserVoucherExpiresAt(uv.getExpiresAt());
                return dto;
            })
            .toList();
    }

    /**
     * Mark voucher đã sử dụng
     */
    public void markVoucherAsUsed(Long userId, String voucherCode, Long orderId) {
        Voucher voucher = voucherRepo.findByCode(voucherCode)
            .orElseThrow(() -> AppException.notFound("Voucher"));

        // Nếu là PERSONAL voucher, update UserVoucher
        if (voucher.getVoucherType() == Voucher.VoucherType.PERSONAL) {
            UserVoucher userVoucher = userVoucherRepo.findByUserIdAndVoucherCode(userId, voucherCode)
                .orElseThrow(() -> AppException.notFound("UserVoucher"));
            
            userVoucher.setStatus(UserVoucher.Status.USED);
            userVoucher.setUsedAt(LocalDateTime.now());
            userVoucher.setOrderId(orderId);
            userVoucherRepo.save(userVoucher);
        }

        // Update voucher usage count
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepo.save(voucher);
    }

    public VoucherDTO create(VoucherRequest request) {
        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw AppException.badRequest("INVALID_DATES", 
                "Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Check duplicate code
        if (voucherRepo.findByCode(request.getCode()).isPresent()) {
            throw AppException.conflict("VOUCHER_EXISTS", "Mã voucher đã tồn tại");
        }

        Voucher voucher = Voucher.builder()
            .code(request.getCode().toUpperCase())
            .name(request.getName())
            .voucherType(request.getVoucherType() != null
                ? Voucher.VoucherType.valueOf(request.getVoucherType())
                : Voucher.VoucherType.PUBLIC)
            .discountType(Voucher.DiscountType.valueOf(request.getDiscountType()))
            .discountValue(request.getDiscountValue())
            .minOrderValue(request.getMinOrderValue())
            .maxDiscount(request.getMaxDiscount())
            .usageLimit(request.getMaxUsageCount())
            .usagePerUser(request.getUsagePerUser() != null ? request.getUsagePerUser() : 1)
            .usedCount(0)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        Voucher saved = voucherRepo.save(voucher);
        return toDTO(saved);
    }

    public VoucherDTO update(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Voucher"));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw AppException.badRequest("INVALID_DATES", 
                "Ngày kết thúc phải sau ngày bắt đầu");
        }

        voucher.setName(request.getName());
        if (request.getVoucherType() != null) {
            voucher.setVoucherType(Voucher.VoucherType.valueOf(request.getVoucherType()));
        }
        voucher.setDiscountType(Voucher.DiscountType.valueOf(request.getDiscountType()));
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getMaxUsageCount());
        if (request.getUsagePerUser() != null) {
            voucher.setUsagePerUser(request.getUsagePerUser());
        }
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        
        if (request.getIsActive() != null) {
            voucher.setIsActive(request.getIsActive());
        }

        Voucher updated = voucherRepo.save(voucher);
        return toDTO(updated);
    }

    // Mapping method
    private VoucherDTO toDTO(Voucher voucher) {
        return VoucherDTO.builder()
            .id(voucher.getId())
            .code(voucher.getCode())
            .name(voucher.getName())
            .voucherType(voucher.getVoucherType().name())
            .discountType(voucher.getDiscountType().name())
            .discountValue(voucher.getDiscountValue())
            .minOrderValue(voucher.getMinOrderValue())
            .maxDiscount(voucher.getMaxDiscount())
            .maxUsageCount(voucher.getUsageLimit())
            .usagePerUser(voucher.getUsagePerUser())
            .usedCount(voucher.getUsedCount())
            .startDate(voucher.getStartDate())
            .endDate(voucher.getEndDate())
            .isActive(voucher.getIsActive())
            .build();
    }
}
