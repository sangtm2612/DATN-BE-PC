package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.entity.Review;
import com.kinhduanpc.entity.ReviewHelpful;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.ReviewHelpfulRepository;
import com.kinhduanpc.repository.ReviewRepository;
import com.kinhduanpc.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Đánh giá sản phẩm")
public class ReviewController {

    private final ReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ReviewHelpfulRepository reviewHelpfulRepo;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Review>>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> result = reviewRepo.findByProductIdAndIsVisibleTrueOrderByCreatedAtDesc(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
            new ApiResponse.PageMeta(page, size, result.getTotalElements(), result.getTotalPages())));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Review>> create(
            @PathVariable Long productId,
            Authentication auth,
            @Valid @RequestBody ReviewRequest req) {

        Long userId = (Long) auth.getPrincipal();

        if (reviewRepo.existsByProductIdAndUserId(productId, userId)) {
            throw AppException.conflict("ALREADY_REVIEWED", "Bạn đã đánh giá sản phẩm này rồi");
        }

        var user    = userRepo.findById(userId)
            .orElseThrow(() -> AppException.notFound("Người dùng"));
        var product = productRepo.findById(productId)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));

        Review review = Review.builder()
            .product(product)
            .user(user)
            .rating(req.getRating())
            .title(req.getTitle())
            .content(req.getContent())
            .isVisible(true)
            .isVerifiedPurchase(false)
            .helpfulCount(0)
            .build();

        return ResponseEntity.ok(ApiResponse.success(reviewRepo.save(review), "Đánh giá thành công"));
    }

    @PostMapping("/{id}/helpful")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleHelpful(
            @PathVariable Long id, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        Review review = reviewRepo.findById(id).orElseThrow(() -> AppException.notFound("Đánh giá"));

        boolean nowHelpful;
        if (reviewHelpfulRepo.existsByUserIdAndReviewId(userId, id)) {
            reviewHelpfulRepo.deleteById(new com.kinhduanpc.entity.ReviewHelpfulId(userId, id));
            nowHelpful = false;
        } else {
            var user = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("Người dùng"));
            try {
                reviewHelpfulRepo.saveAndFlush(ReviewHelpful.builder().user(user).review(review).build());
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Da duoc danh dau boi 1 request dong thoi khac (double-click/race) -> coi nhu thanh cong, khong loi
            }
            nowHelpful = true;
        }

        long count = reviewHelpfulRepo.countByReviewId(id);
        review.setHelpfulCount((int) count);
        reviewRepo.save(review);

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("helpfulCount", count, "isHelpful", nowHelpful),
            nowHelpful ? "Đã đánh dấu hữu ích" : "Đã bỏ đánh dấu hữu ích"));
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "Vui lòng chọn số sao")
        @Min(value = 1, message = "Tối thiểu 1 sao")
        @Max(value = 5, message = "Tối đa 5 sao")
        private Integer rating;
        private String title;
        private String content;
    }
}
