package com.kinhduanpc.repository;

import com.kinhduanpc.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    List<VoucherUsage> findByVoucherIdAndUserId(Long voucherId, Long userId);
}
