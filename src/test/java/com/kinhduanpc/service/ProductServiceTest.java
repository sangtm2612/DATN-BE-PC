package com.kinhduanpc.service;

import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BrandRepository;
import com.kinhduanpc.repository.CategoryRepository;
import com.kinhduanpc.repository.ProductRelatedRepository;
import com.kinhduanpc.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService — covers UT_PRODUCT_01, UT_PRODUCT_02
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private BrandRepository brandRepo;
    @Mock private ProductRelatedRepository productRelatedRepo;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private ProductService productService;

    // ──────────────────────────────────────────────────────────────────────
    // UT_PRODUCT_01: Lấy sản phẩm theo ID hợp lệ → 200, trả về ProductResponse
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_PRODUCT_01 – Lấy sản phẩm theo ID hợp lệ → trả về ProductResponse")
    void getById_withValidId_returnsProduct() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Test Product");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(1_000_000));
        when(product.getOriginalPrice()).thenReturn(null);
        when(product.getImages()).thenReturn(Collections.emptyList());
        when(product.getAttributes()).thenReturn(Collections.emptyList());
        when(product.getTags()).thenReturn(Collections.emptyList());
        when(product.getCategory()).thenReturn(null);
        when(product.getBrand()).thenReturn(null);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(promotionService.getActivePromotions()).thenReturn(List.of());

        ProductResponse result = productService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_PRODUCT_02: Tìm sản phẩm không tồn tại → 404 Not Found
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_PRODUCT_02 – Tìm sản phẩm ID không tồn tại → 404 Not Found")
    void getById_withInvalidId_throwsNotFound() {
        when(productRepo.findById(99999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> productService.getById(99999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }
}
