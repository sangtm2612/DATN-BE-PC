package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.ReviewDTO;
import com.kinhduanpc.dto.ReviewRequest;
import com.kinhduanpc.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Đánh giá sản phẩm")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewDTO>>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewDTO> reviews = reviewService.getReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ReviewDTO>> create(
            @PathVariable Long productId,
            Authentication auth,
            @Valid @RequestBody ReviewRequest request) {
        Long userId = (Long) auth.getPrincipal();
        ReviewDTO review = reviewService.createReview(productId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(review, "Đánh giá thành công"));
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleHelpful(
            @PathVariable Long id, 
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Map<String, Object> result = reviewService.toggleHelpful(id, userId);
        boolean isHelpful = (boolean) result.get("isHelpful");
        String message = isHelpful ? "Đã đánh dấu hữu ích" : "Đã bỏ đánh dấu hữu ích";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }
}
