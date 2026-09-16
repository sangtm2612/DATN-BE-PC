package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private TriggerType triggerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    // ===== Điều kiện đủ =====

    @Column(name = "min_total_spent", precision = 15, scale = 2)
    private BigDecimal minTotalSpent;

    @Column(name = "min_completed_orders")
    private Integer minCompletedOrders;

    @Column(name = "min_account_age_days")
    private Integer minAccountAgeDays;

    @Column(name = "spending_milestone", precision = 15, scale = 2)
    private BigDecimal spendingMilestone;

    @Column(name = "order_count_milestone")
    private Integer orderCountMilestone;

    // ===== Giới hạn =====

    @Column(name = "max_distributions")
    private Integer maxDistributions;

    @Column(name = "distributed_count", nullable = false)
    @Builder.Default
    private Integer distributedCount = 0;

    @Column(name = "one_per_user", nullable = false)
    @Builder.Default
    private Boolean onePerUser = true;

    @Column(name = "custom_expires_days")
    private Integer customExpiresDays;

    // ===== Thời gian hiệu lực =====

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive)
               && now.isAfter(startDate) && now.isBefore(endDate)
               && (maxDistributions == null || distributedCount < maxDistributions);
    }

    public enum TriggerType {
        WELCOME,
        FIRST_ORDER,
        BIRTHDAY,
        SPENDING_MILESTONE,
        ORDER_COUNT,
        REVIEW_REWARD
    }
}
