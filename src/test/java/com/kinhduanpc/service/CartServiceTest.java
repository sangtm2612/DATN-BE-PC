package com.kinhduanpc.service;

import com.kinhduanpc.entity.Cart;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.CartRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CartService — covers UT_CART_01, UT_CART_02
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepo;
    @Mock private ProductRepository productRepo;
    @Mock private UserRepository userRepo;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private CartService cartService;

    // ──────────────────────────────────────────────────────────────────────
    // UT_CART_01: Thêm sản phẩm vào giỏ → CartItem được tạo
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_CART_01 – Thêm sản phẩm hợp lệ vào giỏ → CartItem được tạo")
    void addItem_withValidProduct_addsItemToCart() {
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 2;

        Cart existingCart = Cart.builder().id(1L).build();

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getIsActive()).thenReturn(true);
        when(product.getStockQty()).thenReturn(10);
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(1_000_000));

        when(cartRepo.findAllByUserId(userId)).thenReturn(List.of(existingCart));
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepo.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promotionService.getActivePromotions()).thenReturn(List.of());
        when(promotionService.calculateDiscount(any(), any(), any()))
                .thenReturn(new PromotionService.PromotionCalcResult(BigDecimal.ZERO, BigDecimal.ZERO));

        CartService.CartResponse result = cartService.addItem(userId, null, productId, quantity);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());       // 1 loại sản phẩm trong giỏ
        assertEquals(quantity, result.getTotalItems());  // totalItems = tổng số lượng = 2
        verify(cartRepo, times(1)).save(any(Cart.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_CART_02: Thêm số lượng vượt tồn kho → 400 INSUFFICIENT_STOCK
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_CART_02 – Thêm số lượng vượt tồn kho → 400 INSUFFICIENT_STOCK")
    void addItem_whenQuantityExceedsStock_throwsBadRequest() {
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 5;

        Cart existingCart = Cart.builder().id(1L).build();

        Product product = mock(Product.class);
        when(product.getIsActive()).thenReturn(true);
        when(product.getStockQty()).thenReturn(2);

        when(cartRepo.findAllByUserId(userId)).thenReturn(List.of(existingCart));
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));

        AppException ex = assertThrows(AppException.class,
                () -> cartService.addItem(userId, null, productId, quantity));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode());
        verify(cartRepo, never()).save(any());
    }
}
