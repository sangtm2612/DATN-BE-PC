package com.kinhduanpc.dto.blog;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogCategoryDTO {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
}
