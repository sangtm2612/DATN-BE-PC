package com.kinhduanpc.service;

import com.kinhduanpc.dto.admin.RevenuePointResponse;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final DateTimeFormatter DAY_LABEL   = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL  = DateTimeFormatter.ofPattern("MM/yyyy");

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public Map<String, Object> getDashboardStats() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal monthRevenue = orderRepo.sumRevenueByDateRange(startOfMonth, now);
        monthRevenue = monthRevenue != null ? monthRevenue : BigDecimal.ZERO;
        BigDecimal prevMonthRevenue = orderRepo.sumRevenueByDateRange(startOfPrevMonth, startOfMonth);
        prevMonthRevenue = prevMonthRevenue != null ? prevMonthRevenue : BigDecimal.ZERO;

        BigDecimal revenueChangePercent;
        if (prevMonthRevenue.signum() == 0) {
            revenueChangePercent = monthRevenue.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        } else {
            revenueChangePercent = monthRevenue.subtract(prevMonthRevenue)
                .divide(prevMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        long ordersThisMonth = orderRepo.countByDateRange(startOfMonth, now);
        long ordersToday = orderRepo.countByDateRange(startOfToday, now);
        long totalProducts = productRepo.count();
        long lowStockCount = productRepo.countLowStockProducts();
        long totalCustomers = userRepo.count();
        long newCustomersThisMonth = userRepo.countByCreatedAtBetween(startOfMonth, now);

        Map<String, Object> stats = new HashMap<>();
        stats.put("monthRevenue",           monthRevenue);
        stats.put("revenueChangePercent",   revenueChangePercent);
        stats.put("ordersThisMonth",        ordersThisMonth);
        stats.put("ordersToday",            ordersToday);
        stats.put("totalProducts",          totalProducts);
        stats.put("lowStockCount",          lowStockCount);
        stats.put("totalCustomers",         totalCustomers);
        stats.put("newCustomersThisMonth",  newCustomersThisMonth);
        return stats;
    }

    /** Chuoi doanh thu cho bieu do Dashboard, gom theo ngay (week/month) hoac thang (year). */
    public List<RevenuePointResponse> getRevenueChart(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        LocalDate from;
        boolean byMonth = "year".equals(period);
        if ("week".equals(period)) {
            from = today.minusDays(6);
        } else if (byMonth) {
            from = today.withDayOfMonth(1).minusMonths(11);
        } else {
            from = today.withDayOfMonth(1);
        }

        Map<String, BigDecimal> buckets = new LinkedHashMap<>();
        if (byMonth) {
            for (int i = 0; i < 12; i++) {
                buckets.put(from.plusMonths(i).format(MONTH_LABEL), BigDecimal.ZERO);
            }
        } else {
            long days = ChronoUnit.DAYS.between(from, today) + 1;
            for (long i = 0; i < days; i++) {
                buckets.put(from.plusDays(i).format(DAY_LABEL), BigDecimal.ZERO);
            }
        }

        List<Order> orders = orderRepo.findByStatusAndCreatedAtBetween(
            Order.OrderStatus.completed, from.atStartOfDay(), now);

        for (Order order : orders) {
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            String key = byMonth ? orderDate.withDayOfMonth(1).format(MONTH_LABEL) : orderDate.format(DAY_LABEL);
            BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal net = order.getTotalAmount().subtract(refund);
            buckets.merge(key, net, BigDecimal::add);
        }

        return buckets.entrySet().stream()
            .map(e -> RevenuePointResponse.builder().label(e.getKey()).revenue(e.getValue()).build())
            .toList();
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
