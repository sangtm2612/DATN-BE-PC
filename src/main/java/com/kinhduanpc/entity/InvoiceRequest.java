package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "invoice_type", columnDefinition = "invoice_type", nullable = false)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.personal;

    @Column(name = "buyer_name", nullable = false, length = 200)
    private String buyerName;

    @Column(name = "buyer_address", length = 400)
    private String buyerAddress;

    @Column(name = "buyer_email", nullable = false, length = 200)
    private String buyerEmail;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum InvoiceType { personal, company }
}
