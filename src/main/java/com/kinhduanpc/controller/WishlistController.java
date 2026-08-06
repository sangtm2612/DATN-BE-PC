package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.WishlistDTO;
import com.kinhduanpc.service.WishlistService;
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

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistDTO>>> getWishlist(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlistByUser(userId)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable Long productId, 
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String message = wishlistService.toggleWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @GetMapping("/{productId}/check")
    public ResponseEntity<ApiResponse<Boolean>> check(
            @PathVariable Long productId, 
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            wishlistService.checkInWishlist(userId, productId)));
    }
}
