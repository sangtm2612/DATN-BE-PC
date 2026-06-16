package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Banner;
import com.kinhduanpc.entity.Category;
import com.kinhduanpc.repository.BannerRepository;
import com.kinhduanpc.repository.CategoryRepository;
import com.kinhduanpc.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Dữ liệu trang chủ")
public class HomeController {

    private final ProductService productService;
    private final BannerRepository bannerRepo;
    private final CategoryRepository categoryRepo;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ data trang chủ")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeData() {
        List<Banner> sliders = bannerRepo.findActiveBannersByPosition("home_slider", LocalDateTime.now());
        List<Category> categories = categoryRepo.findRootCategories();
        List<ProductResponse> featured = productService.getFeatured(12);
        List<ProductResponse> newProducts = productService.getNewProducts(12);
        List<ProductResponse> bestSellers = productService.getBestSellers(12);
        List<ProductResponse> onSale = productService.getOnSale(12);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "sliders", sliders,
            "categories", categories,
            "featuredProducts", featured,
            "newProducts", newProducts,
            "bestSellers", bestSellers,
            "onSaleProducts", onSale
        )));
    }
}
