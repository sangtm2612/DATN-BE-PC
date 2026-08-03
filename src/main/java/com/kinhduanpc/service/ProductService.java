package com.kinhduanpc.service;

import com.kinhduanpc.dto.product.ProductResponse;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.CategoryRepository;
import com.kinhduanpc.repository.BrandRepository;
import com.kinhduanpc.repository.ProductRelatedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductRelatedRepository productRelatedRepo;

    public Page<ProductResponse> getProducts(Long categoryId, Long brandId,
                                              BigDecimal minPrice, BigDecimal maxPrice,
                                              String sort, int page, int size) {
        Sort sortObj = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return productRepo.findWithFilters(categoryId, brandId, minPrice, maxPrice, pageable)
                          .map(this::toSummaryResponse);
    }

    public Page<ProductResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepo.searchByKeyword(keyword, pageable).map(this::toSummaryResponse);
    }

    @Transactional
    public ProductResponse getBySlug(String slug) {
        Product p = productRepo.findBySlug(slug)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));
        productRepo.incrementViewCount(p.getId());
        return toDetailResponse(p);
    }

    public ProductResponse getById(Long id) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));
        return toDetailResponse(p);
    }

    public List<ProductResponse> getFeatured(int limit) {
        return productRepo.findFeaturedProducts(PageRequest.of(0, limit))
                          .stream().map(this::toSummaryResponse).toList();
    }

    public List<ProductResponse> getNewProducts(int limit) {
        return productRepo.findNewProducts(PageRequest.of(0, limit))
                          .stream().map(this::toSummaryResponse).toList();
    }

    public List<ProductResponse> getBestSellers(int limit) {
        return productRepo.findBestSellers(PageRequest.of(0, limit))
                          .stream().map(this::toSummaryResponse).toList();
    }

    public List<ProductResponse> getOnSale(int limit) {
        return productRepo.findOnSaleProducts(PageRequest.of(0, limit))
                          .stream().map(this::toSummaryResponse).toList();
    }

    /**
     * curatedOnly=true bo qua fallback cung-category — dung cho UI admin de
     * phan biet "chua gan lien quan nao" voi ket qua goi y tu dong.
     */
    public List<ProductResponse> getRelated(Long productId, int limit, boolean curatedOnly) {
        List<ProductRelated> curated = productRelatedRepo.findByProductIdOrderBySortOrderAsc(productId);
        if (!curated.isEmpty()) {
            return curated.stream()
                .limit(limit)
                .map(pr -> toSummaryResponse(pr.getRelatedProduct()))
                .toList();
        }
        if (curatedOnly) return List.of();
        Product p = productRepo.findById(productId)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));
        return productRepo.findRelatedProducts(p.getCategory().getId(), productId, PageRequest.of(0, limit))
                          .stream().map(this::toSummaryResponse).toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        Category category = categoryRepo.findById(req.getCategoryId())
            .orElseThrow(() -> AppException.notFound("Danh mục"));
        Brand brand = req.getBrandId() != null
            ? brandRepo.findById(req.getBrandId()).orElse(null) : null;

        String slug = generateSlug(req.getName());
        Product p = Product.builder()
            .category(category).brand(brand)
            .name(req.getName()).slug(slug).sku(req.getSku())
            .shortDesc(req.getShortDesc()).description(req.getDescription())
            .thumbnail(req.getThumbnail()).price(req.getPrice())
            .originalPrice(req.getOriginalPrice())
            .stockQty(req.getStockQty() != null ? req.getStockQty() : 0)
            .warrantyMonths(req.getWarrantyMonths() != null ? req.getWarrantyMonths() : 12)
            .warrantyText(req.getWarrantyText())
            .isActive(true).isFeatured(false).isNew(true)
            .build();

        if (req.getOriginalPrice() != null && req.getOriginalPrice().compareTo(req.getPrice()) > 0) {
            p.setIsOnSale(true);
        }

        productRepo.save(p);
        return toDetailResponse(p);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Sản phẩm"));

        if (req.getCategoryId() != null) {
            p.setCategory(categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> AppException.notFound("Danh mục")));
        }
        if (req.getName() != null) p.setName(req.getName());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getOriginalPrice() != null) p.setOriginalPrice(req.getOriginalPrice());
        if (req.getStockQty() != null) p.setStockQty(req.getStockQty());
        if (req.getIsActive() != null) p.setIsActive(req.getIsActive());
        if (req.getIsFeatured() != null) p.setIsFeatured(req.getIsFeatured());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getThumbnail() != null) p.setThumbnail(req.getThumbnail());
        if (req.getWarrantyMonths() != null) p.setWarrantyMonths(req.getWarrantyMonths());

        return toDetailResponse(productRepo.save(p));
    }

    // ---- Mapping ----

    public ProductResponse toSummaryResponse(Product p) {
        Integer discountPct = null;
        if (p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            discountPct = p.getOriginalPrice().subtract(p.getPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(p.getOriginalPrice(), 0, RoundingMode.HALF_UP)
                .intValue();
        }
        return ProductResponse.builder()
            .id(p.getId()).name(p.getName()).slug(p.getSlug()).sku(p.getSku())
            .shortDesc(p.getShortDesc()).thumbnail(p.getThumbnail())
            .price(p.getPrice()).originalPrice(p.getOriginalPrice())
            .isOnSale(p.getIsOnSale()).discountPercent(discountPct)
            .stockQty(p.getStockQty()).soldQty(p.getSoldQty())
            .ratingAvg(p.getRatingAvg()).ratingCount(p.getRatingCount())
            .warrantyMonths(p.getWarrantyMonths()).warrantyText(p.getWarrantyText())
            .isActive(p.getIsActive()).isFeatured(p.getIsFeatured()).isNew(p.getIsNew())
            .createdAt(p.getCreatedAt())
            .category(p.getCategory() != null ? new ProductResponse.CategoryInfo(
                p.getCategory().getId(), p.getCategory().getName(), p.getCategory().getSlug()) : null)
            .brand(p.getBrand() != null ? new ProductResponse.BrandInfo(
                p.getBrand().getId(), p.getBrand().getName(),
                p.getBrand().getSlug(), p.getBrand().getLogoUrl()) : null)
            .build();
    }

    private ProductResponse toDetailResponse(Product p) {
        ProductResponse resp = toSummaryResponse(p);
        resp.setDescription(p.getDescription());

        // Images
        resp.setImages(p.getImages().stream().map(img ->
            new ProductResponse.ImageInfo(img.getId(), img.getImageUrl(),
                img.getAltText(), img.getIsPrimary(), img.getSortOrder())
        ).toList());

        // Attributes grouped
        Map<String, List<ProductResponse.AttributeItem>> grouped = new LinkedHashMap<>();
        for (ProductAttribute attr : p.getAttributes()) {
            grouped.computeIfAbsent(
                attr.getAttributeGroup() != null ? attr.getAttributeGroup() : "Thông số",
                k -> new ArrayList<>()
            ).add(new ProductResponse.AttributeItem(
                attr.getAttributeName(), attr.getAttributeValue(), attr.getUnit()));
        }
        resp.setAttributeGroups(grouped.entrySet().stream()
            .map(e -> new ProductResponse.AttributeGroup(e.getKey(), e.getValue()))
            .toList());

        resp.setTags(p.getTags().stream().map(Tag::getName).toList());

        return resp;
    }

    private Sort buildSort(String sort) {
        if (sort == null) return Sort.by("soldQty").descending();
        return switch (sort) {
            case "price-asc"  -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "newest"     -> Sort.by("createdAt").descending();
            case "rating"     -> Sort.by("ratingAvg").descending();
            case "best-seller"-> Sort.by("soldQty").descending();
            default           -> Sort.by("isFeatured").descending();
        };
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
            .replaceAll("[àáâãäå]", "a").replaceAll("[èéêë]", "e")
            .replaceAll("[ìíîï]", "i").replaceAll("[òóôõö]", "o")
            .replaceAll("[ùúûü]", "u").replaceAll("[ý]", "y")
            .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-")
            .replaceAll("-+", "-").trim();
        // Đảm bảo unique
        String base = slug;
        int i = 1;
        while (productRepo.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    // Inner DTO for create/update
    @lombok.Data
    public static class ProductRequest {
        private Long categoryId;
        private Long brandId;
        private String name;
        private String sku;
        private String shortDesc;
        private String description;
        private String thumbnail;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stockQty;
        private Integer warrantyMonths;
        private String warrantyText;
        private Boolean isActive;
        private Boolean isFeatured;
    }
}
