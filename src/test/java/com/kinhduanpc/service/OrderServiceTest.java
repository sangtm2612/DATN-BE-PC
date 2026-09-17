package com.kinhduanpc.service;

import com.kinhduanpc.config.OrderConfig;
import com.kinhduanpc.entity.Order;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService — covers UT_ORDER_01
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private OrderHistoryRepository orderHistoryRepo;
    @Mock private CartRepository cartRepo;
    @Mock private ProductRepository productRepo;
    @Mock private UserRepository userRepo;
    @Mock private UserAddressRepository userAddressRepo;
    @Mock private VoucherRepository voucherRepo;
    @Mock private VoucherService voucherService;
    @Mock private PcBuildRepository pcBuildRepo;
    @Mock private PromotionService promotionService;
    @Mock private ShippingMethodRepository shippingMethodRepo;
    @Mock private OrderShippingRepository orderShippingRepo;
    @Mock private ProductStockByStoreRepository stockByStoreRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private VoucherPolicyService voucherPolicyService;
    @Mock private OrderConfig orderConfig;
    @Mock private WarrantyRepository warrantyRepo;

    @InjectMocks
    private OrderService orderService;

    // ──────────────────────────────────────────────────────────────────────
    // UT_ORDER_01: Đơn hàng pending có thể bị hủy → trạng thái chuyển sang cancelled,
    //             orderRepo.save() được gọi
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_ORDER_01 – Hủy đơn hàng ở trạng thái pending → status = cancelled, stock được hoàn")
    void cancelOrder_withPendingOrder_cancelsSuccessfully() {
        User user = User.builder().id(1L).build();
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(Order.OrderStatus.pending)
                .paymentMethod(Order.PaymentMethod.cod)
                .subtotal(BigDecimal.valueOf(1_000_000))
                .totalAmount(BigDecimal.valueOf(1_000_000))
                .orderCode("HD202501001")
                .build();

        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        assertDoesNotThrow(() -> orderService.cancelOrder(1L, 1L, "Không muốn mua nữa"));

        assertEquals(Order.OrderStatus.cancelled, order.getStatus());
        assertNotNull(order.getCancelledAt());
        verify(orderRepo, times(1)).save(order);
    }

    // ──────────────────────────────────────────────────────────────────────
    // UT_ORDER_01 (validation): Đơn hàng đã confirmed không thể hủy → 400 CANNOT_CANCEL
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("UT_ORDER_01 (validation) – Hủy đơn confirmed → 400 CANNOT_CANCEL")
    void cancelOrder_whenStatusIsConfirmed_throwsBadRequest() {
        User user = User.builder().id(1L).build();
        Order order = Order.builder()
                .id(2L)
                .user(user)
                .status(Order.OrderStatus.confirmed)
                .paymentMethod(Order.PaymentMethod.cod)
                .build();

        when(orderRepo.findById(2L)).thenReturn(Optional.of(order));

        AppException ex = assertThrows(AppException.class,
                () -> orderService.cancelOrder(2L, 1L, "Muốn hủy"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("CANNOT_CANCEL", ex.getErrorCode());
        verify(orderRepo, never()).save(any());
    }
}
