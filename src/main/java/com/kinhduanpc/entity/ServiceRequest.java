package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warranty_id")
    private Warranty warranty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "service_code", unique = true, length = 30)
    private String serviceCode;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(name = "serial_number", length = 200)
    private String serialNumber;

    @Column(name = "issue_desc", nullable = false, columnDefinition = "TEXT")
    private String issueDesc;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "service_status", nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.received;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "repair_cost", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal repairCost = BigDecimal.ZERO;

    @Column(name = "customer_approved_repair")
    private Boolean customerApprovedRepair;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "appointment_date")
    private LocalDateTime appointmentDate;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "technician_id")
    private Long technicianId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ServiceMedia> media = new ArrayList<>();

    public enum ServiceStatus { received, diagnosing, repairing, waiting_part, done, returned }
}
