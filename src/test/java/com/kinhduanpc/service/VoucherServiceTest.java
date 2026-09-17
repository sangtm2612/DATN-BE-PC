package com.kinhduanpc.service;

import com.kinhduanpc.dto.VoucherDTO;
import com.kinhduanpc.entity.Voucher;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.UserVoucherRepository;
import com.kinhduanpc.repository.VoucherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoucherService — covers UT_VOUCHER_01, UT_VOUCHER_02
 */
@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock private VoucherRepository voucherRepo;
    @Mock private UserVoucherRepository userVoucherRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private VoucherService voucherService;

    // ──────────────────────────────────────────────────────────────────────
    // UT_VOUCHER_01: Áp dụng voucher hợp lệ → trả về VoucherDTO với discountValue đúng
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_VOUCHER_01 – Voucher PUBLIC hợp lệ → trả về VoucherDTO, discountValue = 10%")
    void checkVoucher_withValidPublicVoucher_returnsDTO() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("SALE10")
                .name("Giảm 10%")
                .voucherType(Voucher.VoucherType.PUBLIC)
                .discountType(Voucher.DiscountType.percent)
                .discountValue(BigDecimal.valueOf(10))
                .usedCount(5)
                .usageLimit(100)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        when(voucherRepo.findValidByCode(eq("SALE10"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(voucher));

        VoucherDTO result = voucherService.checkVoucher("SALE10", null);

        assertNotNull(result);
        assertEquals("SALE10", result.getCode());
        assertEquals(BigDecimal.valueOf(10), result.getDiscountValue());
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_VOUCHER_02: Áp dụng voucher hết hạn → 400 INVALID_VOUCHER
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_VOUCHER_02 – Voucher hết hạn / không hợp lệ → 400 INVALID_VOUCHER")
    void checkVoucher_withExpiredVoucher_throwsBadRequest() {
        when(voucherRepo.findValidByCode(eq("EXPIRED"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> voucherService.checkVoucher("EXPIRED", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("INVALID_VOUCHER", ex.getErrorCode());
    }
}
