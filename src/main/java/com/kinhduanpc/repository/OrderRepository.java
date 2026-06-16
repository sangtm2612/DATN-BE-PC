package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    @Query("SELECT o FROM Order o WHERE o.orderCode = :code AND o.shippingPhone = :phone")
    Optional<Order> findByOrderCodeAndPhone(
        @Param("code")  String code,
        @Param("phone") String phone
    );

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdOrderByCreatedAtDesc(
        @Param("userId") Long userId, Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") Order.OrderStatus status,
        Pageable pageable
    );

    // Admin — tất cả đơn
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findAllByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = 'pending' AND o.autoCancelAt IS NOT NULL AND o.autoCancelAt < CURRENT_TIMESTAMP")
    List<Order> findOrdersToAutoCancel();

    @Query("SELECT o FROM Order o WHERE o.status = 'delivered' AND o.deliveredAt < :threshold")
    List<Order> findOrdersToAutoComplete(@Param("threshold") LocalDateTime threshold);

    @Query("""
        SELECT SUM(o.totalAmount) FROM Order o
        WHERE o.status = 'completed'
          AND o.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumRevenueByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    Long countByDateRange(
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to
    );
}
