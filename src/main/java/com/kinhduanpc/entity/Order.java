package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "order_status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", columnDefinition = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", columnDefinition = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.pending;

    @Column(name = "pickup_store_id")
    private Long pickupStoreId;

    @Column(name = "shipping_name", nullable = false, length = 150)
    private String shippingName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    @Column(name = "shipping_province", nullable = false, length = 100)
    private String shippingProvince;

    @Column(name = "shipping_district", nullable = false, length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_ward", nullable = false, length = 100)
    private String shippingWard;

    @Column(name = "shipping_address", nullable = false, length = 300)
    private String shippingAddress;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "build_id")
    private Long buildId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "staff_note", columnDefinition = "TEXT")
    private String staffNote;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "auto_cancel_at")
    private LocalDateTime autoCancelAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public enum OrderStatus {
        pending, confirmed, processing, shipping, delivered, completed, cancelled, refunded
    }

    public enum PaymentMethod {
        cod, bank_transfer, vnpay, momo, zalopay, installment
    }

    public enum PaymentStatus {
        pending, paid, failed, refunded
    }
}
