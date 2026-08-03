package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pc_components")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PcComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_type_id", nullable = false)
    private PcComponentType componentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String socket;

    @Column(length = 50)
    private String chipset;

    @Column(name = "ram_type", length = 20)
    private String ramType;

    @Column(name = "ram_slots")
    private Integer ramSlots;

    @Column(name = "max_ram_gb")
    private Integer maxRamGb;

    @Column(name = "tdp_watts")
    private Integer tdpWatts;

    @Column(name = "psu_wattage")
    private Integer psuWattage;

    @Column(name = "is_like_new", nullable = false)
    @Builder.Default
    private Boolean isLikeNew = false;

    @Column(name = "ram_capacity_gb")
    private Integer ramCapacityGb;

    @Column(name = "ram_speed_mhz")
    private Integer ramSpeedMhz;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "storage_interface", length = 30)
    private String storageInterface;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
