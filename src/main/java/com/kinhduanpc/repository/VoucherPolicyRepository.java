package com.kinhduanpc.repository;

import com.kinhduanpc.entity.VoucherPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VoucherPolicyRepository extends JpaRepository<VoucherPolicy, Long> {

    @Query("""
        SELECT p FROM VoucherPolicy p
        JOIN FETCH p.voucher v
        WHERE p.triggerType = :triggerType
          AND p.isActive = true
          AND :now BETWEEN p.startDate AND p.endDate
          AND (p.maxDistributions IS NULL OR p.distributedCount < p.maxDistributions)
        """)
    List<VoucherPolicy> findActivePoliciesByTrigger(
        VoucherPolicy.TriggerType triggerType, LocalDateTime now);

    @Query("""
        SELECT p FROM VoucherPolicy p
        JOIN FETCH p.voucher v
        WHERE p.isActive = true
          AND :now BETWEEN p.startDate AND p.endDate
        ORDER BY p.createdAt DESC
        """)
    List<VoucherPolicy> findAllActive(LocalDateTime now);
}
