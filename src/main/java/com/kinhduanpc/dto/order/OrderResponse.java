package com.kinhduanpc.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private Long id;
    private String orderCode;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    private String shippingName;
    private String shippingPhone;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String voucherCode;
    private String note;
    private String cancelledReason;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private List<OrderItemResponse> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private Integer warrantyMonths;
    }
}
