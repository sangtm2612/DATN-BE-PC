package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "from_value", columnDefinition = "TEXT")
    private String fromValue;

    @Column(name = "to_value", columnDefinition = "TEXT")
    private String toValue;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_by_name", length = 150)
    private String performedByName;

    @Column(name = "performed_by_role", length = 30)
    private String performedByRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static final String RETURN_REQUEST  = "RETURN_REQUEST";
    public static final String SERVICE_REQUEST = "SERVICE_REQUEST";
    public static final String WARRANTY        = "WARRANTY";
    public static final String VOUCHER_POLICY  = "VOUCHER_POLICY";

    public static final String STATUS_CHANGED      = "STATUS_CHANGED";
    public static final String NOTE_ADDED          = "NOTE_ADDED";
    public static final String COST_QUOTED         = "COST_QUOTED";
    public static final String DIAGNOSIS_SET       = "DIAGNOSIS_SET";
    public static final String TECHNICIAN_ASSIGNED = "TECHNICIAN_ASSIGNED";
    public static final String SERIAL_UPDATED      = "SERIAL_UPDATED";
    public static final String POLICY_CREATED      = "POLICY_CREATED";
    public static final String POLICY_UPDATED      = "POLICY_UPDATED";
    public static final String POLICY_DELETED      = "POLICY_DELETED";
    public static final String POLICY_TOGGLED      = "POLICY_TOGGLED";
}
