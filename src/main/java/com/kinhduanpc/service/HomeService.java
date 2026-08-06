package com.kinhduanpc.service;

import com.kinhduanpc.dto.HomeDataDTO;
import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.Banner;
import com.kinhduanpc.entity.Category;
import com.kinhduanpc.repository.BannerRepository;
import com.kinhduanpc.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HomeService {

    private final ProductService productService;
    private final BannerRepository bannerRepo;
    private final CategoryRepository categoryRepo;

    public HomeDataDTO getHomeData() {
        List<Banner> sliders = bannerRepo.findActiveBannersByPosition("home_slider", LocalDateTime.now());
        List<Category> categories = categoryRepo.findRootCategories();
        List<ProductResponse> featured = productService.getFeatured(12);
        List<ProductResponse> newProducts = productService.getNewProducts(12);
        List<ProductResponse> bestSellers = productService.getBestSellers(12);
        List<ProductResponse> onSale = productService.getOnSale(12);

        return HomeDataDTO.builder()
                .sliders(sliders)
                .categories(categories)
                .featuredProducts(featured)
                .newProducts(newProducts)
                .bestSellers(bestSellers)
                .onSaleProducts(onSale)
                .build();
    }
}
