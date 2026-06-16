package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pc_component_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PcComponentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
