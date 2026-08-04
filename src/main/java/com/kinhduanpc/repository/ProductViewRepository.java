package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    List<ProductView> findByUserIdOrderByViewedAtDesc(Long userId);

    Optional<ProductView> findTopByProductIdAndUserIdOrderByViewedAtDesc(Long productId, Long userId);
    Optional<ProductView> findTopByProductIdAndSessionIdOrderByViewedAtDesc(Long productId, String sessionId);

    // Lay du du lieu tho de dedupe theo san pham o tang service, gioi han hien thi toi da 20 SP
    List<ProductView> findTop50ByUserIdOrderByViewedAtDesc(Long userId);
    List<ProductView> findTop50BySessionIdOrderByViewedAtDesc(String sessionId);
}
