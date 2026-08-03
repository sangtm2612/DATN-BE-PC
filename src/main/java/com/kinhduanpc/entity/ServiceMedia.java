package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "media_type", nullable = false, length = 10)
    @Builder.Default
    private String mediaType = "image";

    @Column(name = "uploaded_by", nullable = false, length = 20)
    @Builder.Default
    private String uploadedBy = "customer";

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
