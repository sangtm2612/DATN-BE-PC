package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    @Query("""
        SELECT v FROM Voucher v
        WHERE v.code      = :code
          AND v.isActive  = true
          AND v.startDate <= :now
          AND v.endDate   >= :now
          AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)
        """)
    Optional<Voucher> findValidByCode(
        @Param("code") String code,
        @Param("now")  LocalDateTime now
    );
}
