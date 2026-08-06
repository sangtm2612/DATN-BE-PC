package com.kinhduanpc.service;

import com.kinhduanpc.dto.VoucherDTO;
import com.kinhduanpc.dto.VoucherRequest;
import com.kinhduanpc.entity.Voucher;
import com.kinhduanpc.exception.AppException;
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

    @Transactional(readOnly = true)
    public List<VoucherDTO> findAll() {
        return voucherRepo.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public VoucherDTO checkVoucher(String code) {
        Voucher voucher = voucherRepo.findValidByCode(code, LocalDateTime.now())
            .orElseThrow(() -> AppException.badRequest("INVALID_VOUCHER", 
                "Voucher không hợp lệ hoặc đã hết hạn"));
        
        // Additional validation
        if (voucher.getUsageLimit() != null && 
            voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw AppException.badRequest("VOUCHER_EXHAUSTED", "Voucher đã hết lượt sử dụng");
        }
        
        return toDTO(voucher);
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
            .discountType(Voucher.DiscountType.valueOf(request.getDiscountType()))
            .discountValue(request.getDiscountValue())
            .minOrderValue(request.getMinOrderValue())
            .usageLimit(request.getMaxUsageCount())
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
        voucher.setDiscountType(Voucher.DiscountType.valueOf(request.getDiscountType()));
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setUsageLimit(request.getMaxUsageCount());
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
            .discountType(voucher.getDiscountType().name())
            .discountValue(voucher.getDiscountValue())
            .minOrderValue(voucher.getMinOrderValue())
            .maxUsageCount(voucher.getUsageLimit())
            .usedCount(voucher.getUsedCount())
            .startDate(voucher.getStartDate())
            .endDate(voucher.getEndDate())
            .isActive(voucher.getIsActive())
            .build();
    }
}
