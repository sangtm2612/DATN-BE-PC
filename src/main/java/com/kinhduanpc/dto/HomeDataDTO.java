package com.kinhduanpc.dto;

import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Banner;
import com.kinhduanpc.entity.Category;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeDataDTO {
    private List<Banner> sliders;
    private List<Category> categories;
    private List<ProductResponse> featuredProducts;
    private List<ProductResponse> newProducts;
    private List<ProductResponse> bestSellers;
    private List<ProductResponse> onSaleProducts;
}
