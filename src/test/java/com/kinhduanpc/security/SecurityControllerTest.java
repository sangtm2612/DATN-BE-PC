package com.kinhduanpc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinhduanpc.config.SecurityConfig;
import com.kinhduanpc.controller.OrderController;
import com.kinhduanpc.controller.ProductController;
import com.kinhduanpc.controller.ReviewController;
import com.kinhduanpc.dto.ReviewRequest;
import com.kinhduanpc.dto.ReviewDTO;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.ProductRelatedRepository;
import com.kinhduanpc.repository.ProductStockByStoreRepository;
import com.kinhduanpc.repository.ProductViewRepository;
import com.kinhduanpc.repository.SearchHistoryRepository;
import com.kinhduanpc.repository.TagRepository;
import com.kinhduanpc.security.JwtUtil;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.service.OrderService;
import com.kinhduanpc.service.ProductService;
import com.kinhduanpc.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests — JWT authentication, SQL Injection, XSS
 */
@WebMvcTest(controllers = {OrderController.class, ProductController.class, ReviewController.class})
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=kinhduanpc-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256",
        "jwt.access-token-expiry=3600000",
        "jwt.refresh-token-expiry=2592000000"
})
class SecurityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtUtil jwtUtil;

    @MockBean OrderService orderService;
    @MockBean ProductService productService;
    @MockBean ReviewService reviewService;
    @MockBean ProductRepository productRepo;
    @MockBean TagRepository tagRepo;
    @MockBean ProductRelatedRepository productRelatedRepo;
    @MockBean ProductStockByStoreRepository stockByStoreRepo;
    @MockBean ProductViewRepository productViewRepo;
    @MockBean SearchHistoryRepository searchHistoryRepo;

    // ──────────────────────────────────────────────────────────────────────
    // JWT: Token hợp lệ → 200 OK
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_JWT_01 – JWT hợp lệ → 200 OK trên endpoint bảo vệ")
    void jwtValid_returns200() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com", "customer");
        when(orderService.getUserOrders(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────
    // JWT: Không có token → 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_JWT_02 – Không có JWT → 401 Unauthorized")
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────
    // JWT: Token hết hạn / sai → 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_JWT_03 – JWT sai/hết hạn → 401 Unauthorized")
    void expiredToken_returns401() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────
    // SQL Injection: input độc hại trong keyword → API trả về 200 bình thường
    //   (Spring Data JPA dùng parameterized query, không bị injection)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_SQL_01 – SQL Injection input trong keyword → 200 OK, không lỗi DB")
    void sqlInjection_inSearchKeyword_returns200() throws Exception {
        String maliciousInput = "' OR '1'='1";
        when(productService.search(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/products/search")
                        .param("keyword", maliciousInput))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────
    // XSS: input script trong nội dung review → API trả 401 (cần xác thực)
    //   Controller từ chối không phải do script mà do chưa đăng nhập —
    //   chứng minh XSS input không bypass security layer.
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_XSS_01 – XSS script trong body review → 401 (auth required, script không được thực thi)")
    void xssPayload_inReviewBody_returns401WithoutScriptExecution() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setRating(5);
        req.setTitle("<script>alert('XSS')</script>");
        req.setContent("Sản phẩm tốt <img src=x onerror=alert('XSS')>");

        mockMvc.perform(post("/reviews/product/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────
    // XSS: với JWT hợp lệ, input script được lưu dưới dạng text thuần (no eval)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SEC_XSS_02 – XSS script với JWT hợp lệ → 200, trả về text nguyên vẹn (không thực thi)")
    void xssPayload_withValidToken_storedAsPlainText() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com", "customer");
        String xssTitle = "<script>alert('XSS')</script>";

        ReviewRequest req = new ReviewRequest();
        req.setRating(4);
        req.setTitle(xssTitle);
        req.setContent("Sản phẩm ổn");

        ReviewDTO responseDto = new ReviewDTO();
        responseDto.setRating(4);
        responseDto.setTitle(xssTitle);   // stored as-is (plain text, not executed)
        responseDto.setContent("Sản phẩm ổn");

        when(reviewService.createReview(any(), any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/reviews/product/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                // Response trả về text nguyên vẹn — escape xảy ra ở frontend khi render
                .andExpect(jsonPath("$.data.title").value(xssTitle));
    }
}
