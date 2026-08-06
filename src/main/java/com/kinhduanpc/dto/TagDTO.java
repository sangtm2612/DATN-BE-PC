package com.kinhduanpc.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TagDTO {
    private Long id;
    private String name;
    private String slug;
}
