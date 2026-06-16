package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findBySerialNumber(String serialNumber);
    List<Warranty> findByUserId(Long userId);

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
