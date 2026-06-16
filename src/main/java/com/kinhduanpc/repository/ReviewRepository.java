package com.kinhduanpc.repository;

import com.kinhduanpc.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isVisible = true")
    Long countByProductId(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isVisible = true")
    Double avgRatingByProductId(Long productId);
}
