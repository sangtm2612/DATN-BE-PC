package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Warranty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findBySerialNumber(String serialNumber);
    List<Warranty> findByUserId(Long userId);
    boolean existsByOrderItemId(Long orderItemId);

    @Query("SELECT w FROM Warranty w JOIN FETCH w.product JOIN FETCH w.user ORDER BY w.createdAt DESC")
    Page<Warranty> findAllWithDetails(Pageable pageable);

    @Query("""
        SELECT w FROM Warranty w
        JOIN w.product p
        JOIN w.user u
        WHERE p.id IN (
            SELECT oi.product.id FROM OrderItem oi
            WHERE oi.order.orderCode = :orderCode
        )
        """)
    List<Warranty> findByOrderCode(String orderCode);
}
