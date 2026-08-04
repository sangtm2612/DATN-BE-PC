package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.isActive = true AND :now BETWEEN p.startDate AND p.endDate
        ORDER BY p.createdAt DESC
        """)
    List<Promotion> findAllActive(@Param("now") LocalDateTime now);
}
