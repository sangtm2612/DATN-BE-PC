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
import java.util.*;
import java.util.stream.Collectors;

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

        // Revenue
        BigDecimal monthRevenue = orderRepo.sumRevenueByDateRange(startOfMonth, now);
        monthRevenue = monthRevenue != null ? monthRevenue : BigDecimal.ZERO;
        BigDecimal prevMonthRevenue = orderRepo.sumRevenueByDateRange(startOfPrevMonth, startOfMonth);
        prevMonthRevenue = prevMonthRevenue != null ? prevMonthRevenue : BigDecimal.ZERO;
        BigDecimal todayRevenue = orderRepo.sumRevenueByDateRange(startOfToday, now);
        todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;

        BigDecimal revenueChangePercent;
        if (prevMonthRevenue.signum() == 0) {
            revenueChangePercent = monthRevenue.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        } else {
            revenueChangePercent = monthRevenue.subtract(prevMonthRevenue)
                .divide(prevMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // Orders — chỉ đếm đơn active (không tính cancelled/refunded)
        long ordersThisMonth = orderRepo.countActiveByDateRange(startOfMonth, now);
        long ordersToday     = orderRepo.countActiveByDateRange(startOfToday, now);
        long completedThisMonth = orderRepo.countByStatusAndDateRange(Order.OrderStatus.completed, startOfMonth, now);
        long cancelledThisMonth = orderRepo.countByStatusAndDateRange(Order.OrderStatus.cancelled, startOfMonth, now);

        // Avg order value
        BigDecimal avgOrderValue = completedThisMonth > 0
            ? monthRevenue.divide(BigDecimal.valueOf(completedThisMonth), 0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Products
        long totalProducts = productRepo.count();
        long lowStockCount = productRepo.countLowStockProducts();

        // Customers — chỉ tính role CUSTOMER, không tính staff/admin
        long totalCustomers = userRepo.countByRole(com.kinhduanpc.entity.User.UserRole.customer);
        long newCustomersThisMonth = userRepo.countByRoleAndDateRange(
            com.kinhduanpc.entity.User.UserRole.customer, startOfMonth, now);

        // Breakdown by status (tất cả thời gian)
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (Object[] row : orderRepo.countGroupByStatus()) {
            ordersByStatus.put(((Order.OrderStatus) row[0]).name(), (Long) row[1]);
        }

        // Doanh thu theo phương thức thanh toán (tháng này)
        Map<String, BigDecimal> revenueByPaymentMethod = new HashMap<>();
        for (Object[] row : orderRepo.revenueByPaymentMethod(startOfMonth, now)) {
            revenueByPaymentMethod.put(((Order.PaymentMethod) row[0]).name(),
                row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("monthRevenue",            monthRevenue);
        stats.put("todayRevenue",            todayRevenue);
        stats.put("revenueChangePercent",    revenueChangePercent);
        stats.put("ordersThisMonth",         ordersThisMonth);
        stats.put("ordersToday",             ordersToday);
        stats.put("completedThisMonth",      completedThisMonth);
        stats.put("cancelledThisMonth",      cancelledThisMonth);
        stats.put("avgOrderValue",           avgOrderValue);
        stats.put("totalProducts",           totalProducts);
        stats.put("lowStockCount",           lowStockCount);
        stats.put("totalCustomers",          totalCustomers);
        stats.put("newCustomersThisMonth",   newCustomersThisMonth);
        stats.put("ordersByStatus",          ordersByStatus);
        stats.put("revenueByPaymentMethod",  revenueByPaymentMethod);
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

    /** Báo cáo doanh thu tổng hợp theo khoảng thời gian tùy chọn. */
    public Map<String, Object> getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to   = toDate.plusDays(1).atStartOfDay(); // exclusive upper bound

        // KPIs
        BigDecimal totalRevenue = orderRepo.sumRevenueByDateRange(from, to);
        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;

        Long totalOrdersRaw = orderRepo.countByDateRange(from, to);
        long totalOrders     = totalOrdersRaw != null ? totalOrdersRaw : 0L;
        Long completedRaw = orderRepo.countByStatusAndDateRange(Order.OrderStatus.completed, from, to);
        long completedOrders = completedRaw != null ? completedRaw : 0L;
        Long cancelledRaw = orderRepo.countByStatusAndDateRange(Order.OrderStatus.cancelled, from, to);
        long cancelledOrders = cancelledRaw != null ? cancelledRaw : 0L;

        BigDecimal avgOrderValue = completedOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long newCustomers = userRepo.countByRoleAndDateRange(
            com.kinhduanpc.entity.User.UserRole.customer, from, to);

        // Doanh thu theo ngày
        Map<String, BigDecimal> dailyBuckets = new LinkedHashMap<>();
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        for (long i = 0; i < days; i++) {
            dailyBuckets.put(fromDate.plusDays(i).format(DAY_LABEL), BigDecimal.ZERO);
        }
        List<Order> completedInRange = orderRepo.findByStatusAndCreatedAtBetween(
            Order.OrderStatus.completed, from, to);
        for (Order order : completedInRange) {
            String key = order.getCreatedAt().toLocalDate().format(DAY_LABEL);
            BigDecimal refund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
            dailyBuckets.merge(key, order.getTotalAmount().subtract(refund), BigDecimal::add);
        }
        List<Map<String, Object>> dailyRevenue = dailyBuckets.entrySet().stream()
            .map(e -> { Map<String, Object> m = new HashMap<>(); m.put("label", e.getKey()); m.put("revenue", e.getValue()); return m; })
            .collect(Collectors.toList());

        // Doanh thu theo phương thức thanh toán (kèm số đơn)
        Map<String, Map<String, Object>> revenueByPaymentMethod = new HashMap<>();
        for (Object[] row : orderRepo.revenueByPaymentMethod(from, to)) {
            String method = ((Order.PaymentMethod) row[0]).name();
            BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            // đếm số đơn cho method này
            long count = completedInRange.stream()
                .filter(o -> o.getPaymentMethod() == row[0])
                .count();
            Map<String, Object> detail = new HashMap<>();
            detail.put("revenue", revenue);
            detail.put("count", count);
            revenueByPaymentMethod.put(method, detail);
        }

        // Danh sách đơn hàng trong kỳ (tất cả trạng thái, filter đúng ở DB)
        List<Map<String, Object>> orders = orderRepo.findAllInDateRange(from, to)
            .stream()
            .map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderCode",     o.getOrderCode());
                m.put("createdAt",     o.getCreatedAt().toString());
                m.put("status",        o.getStatus().name());
                m.put("shippingName",  o.getShippingName());
                m.put("shippingPhone", o.getShippingPhone());
                m.put("paymentMethod", o.getPaymentMethod().name());
                m.put("totalAmount",   o.getTotalAmount());
                return m;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue",            totalRevenue);
        result.put("totalOrders",             totalOrders);
        result.put("completedOrders",         completedOrders);
        result.put("cancelledOrders",         cancelledOrders);
        result.put("avgOrderValue",           avgOrderValue);
        result.put("newCustomers",            newCustomers);
        result.put("dailyRevenue",            dailyRevenue);
        result.put("revenueByPaymentMethod",  revenueByPaymentMethod);
        result.put("orders",                  orders);
        return result;
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
