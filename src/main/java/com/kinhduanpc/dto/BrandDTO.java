package com.kinhduanpc.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrandDTO {
    private Long id;
    private String name;
    private String slug;
    private String logoUrl;
    private String website;
    private String description;
    private Boolean isActive;
}
