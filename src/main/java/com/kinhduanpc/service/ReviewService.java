package com.kinhduanpc.service;

import com.kinhduanpc.dto.ReviewDTO;
import com.kinhduanpc.dto.ReviewRequest;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ReviewHelpfulRepository reviewHelpfulRepo;

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepo.findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(
            productId, pageable);
        return reviews.map(this::toDTO);
    }

    public ReviewDTO createReview(Long productId, Long userId, ReviewRequest request) {
        // Check if user already reviewed this product
        if (reviewRepo.existsByProductIdAndUserId(productId, userId)) {
            throw AppException.conflict("ALREADY_REVIEWED", "Bạn đã đánh giá sản phẩm này rồi");
        }

        User user = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));

        Review review = Review.builder()
            .product(product)
            .user(user)
            .rating(request.getRating())
            .title(request.getTitle())
            .content(request.getContent())
            .isVisible(true)
            .isVerifiedPurchase(false) // TODO: Check if user bought this product
            .helpfulCount(0)
            .build();

        Review saved = reviewRepo.save(review);
        return toDTO(saved);
    }

    @Transactional
    public Map<String, Object> toggleHelpful(Long reviewId, Long userId) {
        Review review = reviewRepo.findById(reviewId)
            .orElseThrow(() -> AppException.notFound("Đánh giá"));

        boolean nowHelpful;
        if (reviewHelpfulRepo.existsByUserIdAndReviewId(userId, reviewId)) {
            reviewHelpfulRepo.deleteById(new ReviewHelpfulId(userId, reviewId));
            nowHelpful = false;
        } else {
            User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng"));
            try {
                reviewHelpfulRepo.saveAndFlush(
                    ReviewHelpful.builder()
                        .user(user)
                        .review(review)
                        .build()
                );
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Race condition: already marked by concurrent request
                // Treat as success, no error
            }
            nowHelpful = true;
        }

        // Update helpful count
        long count = reviewHelpfulRepo.countByReviewId(reviewId);
        review.setHelpfulCount((int) count);
        reviewRepo.save(review);

        Map<String, Object> result = new HashMap<>();
        result.put("helpfulCount", count);
        result.put("isHelpful", nowHelpful);
        return result;
    }

    // Mapping method
    private ReviewDTO toDTO(Review review) {
        return ReviewDTO.builder()
            .id(review.getId())
            .productId(review.getProduct().getId())
            .productName(review.getProduct().getName())
            .userId(review.getUser().getId())
            .userName(review.getUser().getFullName())
            .rating(review.getRating())
            .title(review.getTitle())
            .content(review.getContent())
            .isVisible(review.getIsVisible())
            .isVerifiedPurchase(review.getIsVerifiedPurchase())
            .helpfulCount(review.getHelpfulCount())
            .createdAt(review.getCreatedAt())
            .build();
    }
}
