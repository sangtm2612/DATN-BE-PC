package com.kinhduanpc.repository;

import com.kinhduanpc.entity.ReviewHelpful;
import com.kinhduanpc.entity.ReviewHelpfulId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, ReviewHelpfulId> {
    boolean existsByUserIdAndReviewId(Long userId, Long reviewId);
    long countByReviewId(Long reviewId);
}
