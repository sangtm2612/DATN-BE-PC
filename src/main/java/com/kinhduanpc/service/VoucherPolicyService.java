package com.kinhduanpc.service;

import com.kinhduanpc.dto.voucher.VoucherPolicyRequest;
import com.kinhduanpc.dto.voucher.VoucherPolicyResponse;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherPolicyService {

    private final VoucherPolicyRepository policyRepo;
    private final VoucherRepository voucherRepo;
    private final UserVoucherRepository userVoucherRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final NotificationService notificationService;

    // ==================== ADMIN CRUD ====================

    @Transactional(readOnly = true)
    public List<VoucherPolicyResponse> findAll() {
        return policyRepo.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public VoucherPolicyResponse findById(Long id) {
        VoucherPolicy policy = policyRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Chính sách voucher"));
        return toResponse(policy);
    }

    public VoucherPolicyResponse create(VoucherPolicyRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw AppException.badRequest("INVALID_DATES", "Ngày kết thúc phải sau ngày bắt đầu");
        }

        Voucher voucher = voucherRepo.findById(req.getVoucherId())
            .orElseThrow(() -> AppException.notFound("Voucher"));

        if (voucher.getVoucherType() != Voucher.VoucherType.PERSONAL) {
            throw AppException.badRequest("INVALID_VOUCHER_TYPE",
                "Chỉ có thể tạo chính sách với voucher loại PERSONAL");
        }

        VoucherPolicy.TriggerType triggerType =
            VoucherPolicy.TriggerType.valueOf(req.getTriggerType());

        VoucherPolicy policy = VoucherPolicy.builder()
            .name(req.getName())
            .description(req.getDescription())
            .triggerType(triggerType)
            .voucher(voucher)
            .minTotalSpent(req.getMinTotalSpent())
            .minCompletedOrders(req.getMinCompletedOrders())
            .minAccountAgeDays(req.getMinAccountAgeDays())
            .spendingMilestone(req.getSpendingMilestone())
            .orderCountMilestone(req.getOrderCountMilestone())
            .maxDistributions(req.getMaxDistributions())
            .onePerUser(req.getOnePerUser() != null ? req.getOnePerUser() : true)
            .customExpiresDays(req.getCustomExpiresDays())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .isActive(req.getIsActive() != null ? req.getIsActive() : true)
            .build();

        return toResponse(policyRepo.save(policy));
    }

    public VoucherPolicyResponse update(Long id, VoucherPolicyRequest req) {
        VoucherPolicy policy = policyRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Chính sách voucher"));

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw AppException.badRequest("INVALID_DATES", "Ngày kết thúc phải sau ngày bắt đầu");
        }

        Voucher voucher = voucherRepo.findById(req.getVoucherId())
            .orElseThrow(() -> AppException.notFound("Voucher"));

        policy.setName(req.getName());
        policy.setDescription(req.getDescription());
        policy.setTriggerType(VoucherPolicy.TriggerType.valueOf(req.getTriggerType()));
        policy.setVoucher(voucher);
        policy.setMinTotalSpent(req.getMinTotalSpent());
        policy.setMinCompletedOrders(req.getMinCompletedOrders());
        policy.setMinAccountAgeDays(req.getMinAccountAgeDays());
        policy.setSpendingMilestone(req.getSpendingMilestone());
        policy.setOrderCountMilestone(req.getOrderCountMilestone());
        policy.setMaxDistributions(req.getMaxDistributions());
        policy.setCustomExpiresDays(req.getCustomExpiresDays());
        policy.setStartDate(req.getStartDate());
        policy.setEndDate(req.getEndDate());

        if (req.getOnePerUser() != null) policy.setOnePerUser(req.getOnePerUser());
        if (req.getIsActive() != null) policy.setIsActive(req.getIsActive());

        return toResponse(policyRepo.save(policy));
    }

    public void delete(Long id) {
        VoucherPolicy policy = policyRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Chính sách voucher"));
        policyRepo.delete(policy);
    }

    // ==================== TRIGGER: Sự kiện kích hoạt ====================

    /**
     * Gọi khi user đăng ký tài khoản mới.
     * Tìm tất cả policy WELCOME đang active → phát voucher.
     */
    @Async
    public void onUserRegistered(Long userId) {
        triggerPolicies(VoucherPolicy.TriggerType.WELCOME, userId);
    }

    /**
     * Gọi khi đơn hàng chuyển sang trạng thái completed.
     * Kiểm tra FIRST_ORDER, SPENDING_MILESTONE, ORDER_COUNT.
     */
    @Async
    public void onOrderCompleted(Long userId) {
        triggerPolicies(VoucherPolicy.TriggerType.FIRST_ORDER, userId);
        triggerPolicies(VoucherPolicy.TriggerType.SPENDING_MILESTONE, userId);
        triggerPolicies(VoucherPolicy.TriggerType.ORDER_COUNT, userId);
    }

    /**
     * Gọi khi user viết đánh giá sản phẩm.
     */
    @Async
    public void onReviewCreated(Long userId) {
        triggerPolicies(VoucherPolicy.TriggerType.REVIEW_REWARD, userId);
    }

    /**
     * Gọi bởi scheduler hàng ngày — quét user có sinh nhật hôm nay.
     */
    public void processBirthdayVouchers() {
        List<VoucherPolicy> policies = policyRepo.findActivePoliciesByTrigger(
            VoucherPolicy.TriggerType.BIRTHDAY, LocalDateTime.now());

        if (policies.isEmpty()) return;

        List<User> birthdayUsers = userRepo.findUsersWithBirthdayToday();
        for (User user : birthdayUsers) {
            for (VoucherPolicy policy : policies) {
                tryDistribute(policy, user);
            }
        }
    }

    // ==================== LOGIC PHÂN PHỐI ====================

    private void triggerPolicies(VoucherPolicy.TriggerType triggerType, Long userId) {
        List<VoucherPolicy> policies = policyRepo.findActivePoliciesByTrigger(
            triggerType, LocalDateTime.now());

        if (policies.isEmpty()) return;

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        for (VoucherPolicy policy : policies) {
            tryDistribute(policy, user);
        }
    }

    private void tryDistribute(VoucherPolicy policy, User user) {
        try {
            if (!policy.isCurrentlyActive()) return;

            // Kiểm tra onePerUser: user đã nhận voucher này qua policy này chưa
            if (Boolean.TRUE.equals(policy.getOnePerUser())) {
                boolean alreadyHas = userVoucherRepo.existsByUserIdAndVoucherId(
                    user.getId(), policy.getVoucher().getId());
                if (alreadyHas) return;
            }

            // Kiểm tra các điều kiện
            if (!checkEligibility(policy, user)) return;

            // Phát voucher
            distributeVoucher(policy, user);

        } catch (Exception e) {
            log.error("Lỗi phân phối voucher policy={} user={}: {}",
                policy.getId(), user.getId(), e.getMessage());
        }
    }

    private boolean checkEligibility(VoucherPolicy policy, User user) {
        // Tuổi tài khoản
        if (policy.getMinAccountAgeDays() != null) {
            long accountDays = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
            if (accountDays < policy.getMinAccountAgeDays()) return false;
        }

        // Tổng chi tiêu tối thiểu
        if (policy.getMinTotalSpent() != null) {
            BigDecimal totalSpent = orderRepo.sumTotalSpentByUserId(user.getId());
            if (totalSpent.compareTo(policy.getMinTotalSpent()) < 0) return false;
        }

        // Số đơn hoàn thành tối thiểu
        if (policy.getMinCompletedOrders() != null) {
            int completedOrders = orderRepo.countCompletedByUserId(user.getId());
            if (completedOrders < policy.getMinCompletedOrders()) return false;
        }

        // FIRST_ORDER: chỉ khi đây là đơn hoàn thành đầu tiên
        if (policy.getTriggerType() == VoucherPolicy.TriggerType.FIRST_ORDER) {
            int completedOrders = orderRepo.countCompletedByUserId(user.getId());
            if (completedOrders != 1) return false;
        }

        // SPENDING_MILESTONE: tổng chi tiêu phải đạt mốc
        if (policy.getTriggerType() == VoucherPolicy.TriggerType.SPENDING_MILESTONE
            && policy.getSpendingMilestone() != null) {
            BigDecimal totalSpent = orderRepo.sumTotalSpentByUserId(user.getId());
            if (totalSpent.compareTo(policy.getSpendingMilestone()) < 0) return false;
        }

        // ORDER_COUNT: số đơn hoàn thành phải đạt mốc
        if (policy.getTriggerType() == VoucherPolicy.TriggerType.ORDER_COUNT
            && policy.getOrderCountMilestone() != null) {
            int completedOrders = orderRepo.countCompletedByUserId(user.getId());
            if (completedOrders < policy.getOrderCountMilestone()) return false;
        }

        return true;
    }

    private void distributeVoucher(VoucherPolicy policy, User user) {
        LocalDateTime expiresAt = null;
        if (policy.getCustomExpiresDays() != null) {
            expiresAt = LocalDateTime.now().plusDays(policy.getCustomExpiresDays());
        }

        UserVoucher userVoucher = UserVoucher.builder()
            .user(user)
            .voucher(policy.getVoucher())
            .status(UserVoucher.Status.AVAILABLE)
            .expiresAt(expiresAt)
            .build();

        userVoucherRepo.save(userVoucher);

        // Cập nhật số lượt phân phối
        policy.setDistributedCount(policy.getDistributedCount() + 1);
        policyRepo.save(policy);

        // Gửi thông báo
        String title = "Bạn nhận được voucher mới!";
        String message = String.format("Voucher %s — %s đã được thêm vào ví của bạn.",
            policy.getVoucher().getCode(), policy.getName());
        notificationService.createNotification(
            user.getId(), "promotion", title, message, "voucher", policy.getVoucher().getId());

        log.info("Phát voucher {} cho user {} qua policy {}",
            policy.getVoucher().getCode(), user.getId(), policy.getName());
    }

    // ==================== MAPPING ====================

    private VoucherPolicyResponse toResponse(VoucherPolicy p) {
        Voucher v = p.getVoucher();
        return VoucherPolicyResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .description(p.getDescription())
            .triggerType(p.getTriggerType().name())
            .voucherId(v.getId())
            .voucherCode(v.getCode())
            .voucherName(v.getName())
            .discountType(v.getDiscountType().name())
            .discountValue(v.getDiscountValue())
            .minTotalSpent(p.getMinTotalSpent())
            .minCompletedOrders(p.getMinCompletedOrders())
            .minAccountAgeDays(p.getMinAccountAgeDays())
            .spendingMilestone(p.getSpendingMilestone())
            .orderCountMilestone(p.getOrderCountMilestone())
            .maxDistributions(p.getMaxDistributions())
            .distributedCount(p.getDistributedCount())
            .onePerUser(p.getOnePerUser())
            .customExpiresDays(p.getCustomExpiresDays())
            .startDate(p.getStartDate())
            .endDate(p.getEndDate())
            .isActive(p.getIsActive())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
