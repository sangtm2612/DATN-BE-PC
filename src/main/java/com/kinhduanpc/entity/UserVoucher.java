package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng lưu voucher được phân phối cho user cụ thể
 * Dùng để quản lý voucher personal (chỉ user được tặng mới dùng được)
 */
@Entity
@Table(name = "user_vouchers", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "voucher_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    @Column(name = "assigned_by")
    private Long assignedBy; // Admin ID người tặng

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "order_id")
    private Long orderId; // Đơn hàng đã sử dụng voucher này

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Có thể set expiry riêng cho từng user

    /**
     * Kiểm tra voucher còn khả dụng không
     */
    public boolean isAvailable() {
        if (status != Status.AVAILABLE) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return voucher != null && voucher.isValid();
    }

    public enum Status {
        AVAILABLE,  // Chưa dùng, còn hạn
        USED,       // Đã sử dụng
        EXPIRED     // Hết hạn
    }
}
