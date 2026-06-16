package com.kinhduanpc.service;

import com.kinhduanpc.entity.Order;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public Map<String, Object> getDashboardStats() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal monthRevenue = orderRepo.sumRevenueByDateRange(startOfMonth, now);
        long pendingOrders = orderRepo.countByDateRange(startOfMonth, now);
        long totalProducts = productRepo.count();
        long totalCustomers = userRepo.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("monthRevenue",    monthRevenue != null ? monthRevenue : BigDecimal.ZERO);
        stats.put("pendingOrders",   pendingOrders);
        stats.put("totalProducts",   totalProducts);
        stats.put("totalCustomers",  totalCustomers);
        return stats;
    }

    /** Tự động hủy đơn COD quá 48h */
    @Transactional
    public void autoCancelExpiredOrders() {
        orderRepo.findOrdersToAutoCancel().forEach(order -> {
            order.setStatus(Order.OrderStatus.cancelled);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelledReason("Tự động hủy — COD chưa xác nhận sau 48 giờ");
            // Hoàn tồn kho
            order.getItems().forEach(item -> {
                item.getProduct().setStockQty(
                    item.getProduct().getStockQty() + item.getQuantity()
                );
            });
            orderRepo.save(order);
        });
    }

    /** Tự động hoàn thành đơn đã giao sau 7 ngày */
    @Transactional
    public void autoCompleteDeliveredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        orderRepo.findOrdersToAutoComplete(threshold).forEach(order -> {
            order.setStatus(Order.OrderStatus.completed);
            order.setCompletedAt(LocalDateTime.now());
            orderRepo.save(order);
        });
    }
}
