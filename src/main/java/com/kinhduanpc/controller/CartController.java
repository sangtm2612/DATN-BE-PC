package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Giỏ hàng")
public class CartController {

    private final CartService cartService;

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }

    @GetMapping
    @Operation(summary = "Lấy giỏ hàng")
    public ResponseEntity<ApiResponse<CartService.CartResponse>> getCart(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(getUserId(auth), sessionId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ")
    public ResponseEntity<ApiResponse<CartService.CartResponse>> addItem(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
            cartService.addItem(getUserId(auth), sessionId, productId, quantity)));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Cập nhật số lượng")
    public ResponseEntity<ApiResponse<CartService.CartResponse>> updateItem(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
            cartService.updateItem(getUserId(auth), sessionId, productId, quantity)));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ")
    public ResponseEntity<ApiResponse<CartService.CartResponse>> removeItem(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
            cartService.removeItem(getUserId(auth), sessionId, productId)));
    }

    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<CartService.CartResponse>> clearCart(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.clearCart(getUserId(auth), sessionId)));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge giỏ hàng guest sau đăng nhập")
    public ResponseEntity<ApiResponse<Void>> mergeCart(
            Authentication auth,
            @RequestHeader(value = "X-Session-Id") String sessionId) {
        cartService.mergeCart(getUserId(auth), sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Merge giỏ hàng thành công"));
    }
}
