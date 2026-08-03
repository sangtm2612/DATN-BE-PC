package com.kinhduanpc.repository;

import com.kinhduanpc.entity.OrderInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInstallmentRepository extends JpaRepository<OrderInstallment, Long> {
    List<OrderInstallment> findByOrderId(Long orderId);
}
