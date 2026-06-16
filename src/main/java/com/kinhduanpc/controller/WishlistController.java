package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Sản phẩm yêu thích")
public class WishlistController {

    private final WishlistRepository wishlistRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Wishlist>>> getWishlist(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(wishlistRepo.findByUserId(userId)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable Long productId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        if (wishlistRepo.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepo.deleteByUserIdAndProductId(userId, productId);
            return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa khỏi yêu thích"));
        } else {
            User user = userRepo.findById(userId).orElseThrow();
            Product product = productRepo.findById(productId)
                .orElseThrow(() -> AppException.notFound("Sản phẩm"));
            wishlistRepo.save(Wishlist.builder().user(user).product(product).build());
            return ResponseEntity.ok(ApiResponse.success(null, "Đã thêm vào yêu thích"));
        }
    }

    @GetMapping("/{productId}/check")
    public ResponseEntity<ApiResponse<Boolean>> check(
            @PathVariable Long productId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            wishlistRepo.existsByUserIdAndProductId(userId, productId)));
    }
}
