package com.kinhduanpc.scheduler;

import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.OrderHistory;
import com.kinhduanpc.repository.OrderHistoryRepository;
import com.kinhduanpc.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    /**
     * Tự động hủy các đơn hàng COD chưa thanh toán cọc sau 15 phút — chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cancelUnpaidDepositOrders() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);

        List<Order> unpaidOrders = orderRepository.findByStatusAndCreatedAtBefore(
                Order.OrderStatus.pending_deposit,
                cutoffTime
        );

        if (unpaidOrders.isEmpty()) return;

        log.info("Found {} unpaid deposit orders to cancel", unpaidOrders.size());

        for (Order order : unpaidOrders) {
            Order.OrderStatus fromStatus = order.getStatus();
            order.setStatus(Order.OrderStatus.cancelled);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelledReason("Tự động hủy do không thanh toán cọc sau 15 phút");

            // Hoàn lại tồn kho
            order.getItems().forEach(item -> {
                var product = item.getProduct();
                product.setStockQty(product.getStockQty() + item.getQuantity());
                product.setSoldQty(Math.max(0, product.getSoldQty() - item.getQuantity()));
            });

            orderRepository.save(order);

            // Ghi audit trail
            orderHistoryRepository.save(OrderHistory.builder()
                .order(order)
                .fromStatus(fromStatus.name())
                .toStatus(Order.OrderStatus.cancelled.name())
                .actorType("system")
                .note("Tự động hủy: không thanh toán cọc trong 15 phút")
                .build());

            log.info("Auto-cancelled order {} (unpaid deposit)", order.getOrderCode());
        }
    }
}
