package com.kinhduanpc.scheduler;

import com.kinhduanpc.repository.CartRepository;
import com.kinhduanpc.repository.UserTokenRepository;
import com.kinhduanpc.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final AdminService adminService;
    private final CartRepository cartRepo;
    private final UserTokenRepository tokenRepo;

    /** Mỗi 30 phút — tự động hủy đơn COD quá hạn */
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void autoCancelOrders() {
        log.debug("Running auto-cancel orders job");
        adminService.autoCancelExpiredOrders();
    }

    /** Mỗi 1 giờ — tự động hoàn thành đơn đã giao 7 ngày */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void autoCompleteOrders() {
        log.debug("Running auto-complete orders job");
        adminService.autoCompleteDeliveredOrders();
    }

    /** Hàng ngày lúc 2h sáng — dọn dẹp giỏ hàng và token hết hạn */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanup() {
        log.info("Running daily cleanup job");
        cartRepo.deleteExpiredGuestCarts(LocalDateTime.now());
        tokenRepo.deleteExpiredTokens(LocalDateTime.now());
    }
}
