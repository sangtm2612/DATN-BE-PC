package com.kinhduanpc.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blog_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlogCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
