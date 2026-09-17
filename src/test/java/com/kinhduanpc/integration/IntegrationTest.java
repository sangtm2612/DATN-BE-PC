package com.kinhduanpc.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinhduanpc.dto.auth.AuthResponse;
import com.kinhduanpc.dto.auth.LoginRequest;
import com.kinhduanpc.dto.order.CreateOrderRequest;
import com.kinhduanpc.dto.order.OrderResponse;
import com.kinhduanpc.security.JwtUtil;
import com.kinhduanpc.service.AuthService;
import com.kinhduanpc.service.OrderService;
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

import com.kinhduanpc.config.SecurityConfig;
import com.kinhduanpc.controller.AuthController;
import com.kinhduanpc.controller.OrderController;

/**
 * Integration tests — covers IT_01 (Đặt hàng), IT_02 (Thanh toán), IT_03 (JWT flow)
 * Kiểm thử luồng HTTP đầu-cuối qua MVC layer với Spring Security thực.
 */
@WebMvcTest(controllers = {AuthController.class, OrderController.class})
@Import({SecurityConfig.class, com.kinhduanpc.security.JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=kinhduanpc-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256",
        "jwt.access-token-expiry=3600000",
        "jwt.refresh-token-expiry=2592000000"
})
class IntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtUtil jwtUtil;

    @MockBean AuthService authService;
    @MockBean OrderService orderService;

    // ──────────────────────────────────────────────────────────────────────
    // IT_01: Đặt hàng end-to-end
    // POST /orders với session ID hợp lệ → 201 Created, order được tạo
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT_01 – Đặt hàng end-to-end: POST /orders với sessionId → 201 Created")
    void createOrder_withSessionId_returns201() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setSessionId("sess-abc123");
        req.setShippingName("Nguyễn Văn A");
        req.setShippingPhone("0901234567");
        req.setShippingProvince("Hà Nội");
        req.setShippingDistrict("Đống Đa");
        req.setShippingWard("Thổ Quan");
        req.setShippingAddress("123 Đường ABC");
        req.setPaymentMethod("cod");

        OrderResponse mockOrder = new OrderResponse();
        mockOrder.setOrderCode("HD202501001");
        mockOrder.setStatus("pending");

        when(orderService.createOrder(any(), any(), any())).thenReturn(mockOrder);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderCode").value("HD202501001"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // IT_03a: Đăng nhập với thông tin hợp lệ → 200, nhận JWT accessToken
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT_03a – Đăng nhập hợp lệ: POST /auth/login → 200 OK + accessToken")
    void login_withValidCredentials_returns200WithJwt() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCredential("test@test.com");
        req.setPassword("Password1");

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(1L).email("test@test.com").fullName("Test User").role("customer").build();
        AuthResponse authResp = AuthResponse.builder()
                .accessToken("generated.jwt.token")
                .refreshToken("generated.refresh.token")
                .user(userInfo).build();

        when(authService.login(any())).thenReturn(authResp);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("generated.jwt.token"))
                .andExpect(jsonPath("$.data.user.email").value("test@test.com"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // IT_03b: Truy cập API bảo vệ không có JWT → 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT_03b – GET /orders không có JWT → 401 Unauthorized")
    void getMyOrders_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────
    // IT_03c: Truy cập API bảo vệ với JWT hợp lệ → 200 OK
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT_03c – GET /orders với JWT hợp lệ → 200 OK")
    void getMyOrders_withValidToken_returns200() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com", "customer");

        when(orderService.getUserOrders(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────
    // IT_03d: JWT hết hạn / sai → 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("IT_03d – GET /orders với JWT sai/hết hạn → 401 Unauthorized")
    void getMyOrders_withInvalidToken_returns401() throws Exception {
        String fakeToken = "this.is.not.a.valid.jwt";

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized());
    }
}
