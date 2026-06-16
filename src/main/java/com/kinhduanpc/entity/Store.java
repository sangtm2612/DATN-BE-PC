package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 250)
    private String slug;

    @Column(nullable = false, length = 400)
    private String address;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(length = 100)
    private String district;

    @Column(length = 20)
    private String phone;

    @Column(name = "warranty_phone", length = 20)
    private String warrantyPhone;

    @Column(length = 150)
    private String email;

    @Column(name = "open_hours", length = 200)
    private String openHours;

    @Column(name = "break_hours", length = 100)
    private String breakHours;

    // Schema column: map_url
    @Column(name = "map_url", length = 500)
    private String googleMapsUrl;

    // Schema column: latitude
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal lat;

    // Schema column: longitude
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "store_type", length = 30)
    @Builder.Default
    private String storeType = "showroom";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StoreImage> images = new ArrayList<>();
}
