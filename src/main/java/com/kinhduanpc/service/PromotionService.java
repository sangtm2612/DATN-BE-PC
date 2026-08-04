package com.kinhduanpc.service;

import com.kinhduanpc.dto.promotion.PromotionRequest;
import com.kinhduanpc.dto.promotion.PromotionResponse;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BrandRepository;
import com.kinhduanpc.repository.CategoryRepository;
import com.kinhduanpc.repository.PcComponentRepository;
import com.kinhduanpc.repository.ProductRepository;
import com.kinhduanpc.repository.PromotionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final PcComponentRepository pcComponentRepo;

    // ─── Auto-apply (Cart/Order) ───────────────────────────────

    public List<Promotion> getActivePromotions() {
        return promotionRepo.findAllActive(LocalDateTime.now());
    }

    /**
     * Tinh discount tu dong cho danh sach san pham trong gio hang/don hang.
     * Neu 1 san pham khop nhieu promotion (theo san pham/danh muc/thuong hieu/toan bo),
     * chon promotion cho discount CO LOI NHAT cho khach hang (tranh khieu nai khi
     * khach thay 1 promotion "hop dan hon" nhung khong duoc ap dung).
     */
    public PromotionCalcResult calculateDiscount(List<LineItem> items, BigDecimal cartSubtotal, PcBuild build) {
        if (items.isEmpty()) {
            return new PromotionCalcResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Promotion> active = getActivePromotions().stream()
            .filter(p -> cartSubtotal.compareTo(p.getMinOrderValue()) >= 0)
            .toList();
        if (active.isEmpty()) {
            return new PromotionCalcResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        boolean isBuildPc = build != null;
        boolean hasVga = isBuildPc && build.getItems().stream()
            .anyMatch(i -> "vga".equalsIgnoreCase(i.getComponentType().getSlug()));

        // San pham thuoc chinh cau hinh build da luu — chan truong hop khach gui buildId cua
        // 1 build co VGA nhung gio hang thuc te chua chuan (vd them CPU khac, bo VGA) de "muon"
        // muc giam gia CPU cao hon hoac ap dung uu dai build_pc cho don khong that su tu Build PC.
        Set<Long> buildProductIds = isBuildPc
            ? build.getItems().stream().map(i -> i.getProduct().getId()).collect(Collectors.toSet())
            : Set.of();

        Set<Long> cpuProductIds = isBuildPc
            ? pcComponentRepo.findByProductIdIn(items.stream().map(i -> i.getProduct().getId()).toList()).stream()
                .filter(c -> "cpu".equalsIgnoreCase(c.getComponentType().getSlug()))
                .map(c -> c.getProduct().getId())
                .filter(buildProductIds::contains)
                .collect(Collectors.toSet())
            : Set.of();

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal cashBonus = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal lineSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            List<Promotion> matching = active.stream().filter(p -> matchesScope(p, item.getProduct())).toList();

            BigDecimal bestDiscount = BigDecimal.ZERO;
            for (Promotion p : matching) {
                BigDecimal d = calcLineDiscount(p, lineSubtotal);
                if (d.compareTo(bestDiscount) > 0) bestDiscount = d;
            }
            totalDiscount = totalDiscount.add(bestDiscount);

            if (isBuildPc && cpuProductIds.contains(item.getProduct().getId())) {
                Promotion buildPromo = matching.stream()
                    .filter(p -> p.getPromotionType() == Promotion.PromotionType.build_pc)
                    .findFirst().orElse(null);
                if (buildPromo != null) {
                    BigDecimal pct = hasVga ? buildPromo.getBuildpcMaxCpuDiscountPct() : buildPromo.getBuildpcMinCpuDiscountPct();
                    if (pct != null) {
                        totalDiscount = totalDiscount.add(
                            lineSubtotal.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    }
                    if (buildPromo.getBuildpcCashBonus() != null) {
                        BigDecimal bonus = buildPromo.getBuildpcCashBonus();
                        if (buildPromo.getBuildpcMaxCashBonus() != null
                                && bonus.compareTo(buildPromo.getBuildpcMaxCashBonus()) > 0) {
                            bonus = buildPromo.getBuildpcMaxCashBonus();
                        }
                        if (bonus.compareTo(cashBonus) > 0) cashBonus = bonus; // 1 lan/don, khong nhan theo so item
                    }
                }
            }
        }

        return new PromotionCalcResult(totalDiscount, cashBonus);
    }

    private boolean matchesScope(Promotion p, Product product) {
        boolean noScope = p.getProducts().isEmpty() && p.getCategories().isEmpty() && p.getBrands().isEmpty();
        if (noScope) return true;
        boolean productMatch = p.getProducts().stream().anyMatch(x -> x.getId().equals(product.getId()));
        boolean categoryMatch = product.getCategory() != null
            && p.getCategories().stream().anyMatch(x -> x.getId().equals(product.getCategory().getId()));
        boolean brandMatch = product.getBrand() != null
            && p.getBrands().stream().anyMatch(x -> x.getId().equals(product.getBrand().getId()));
        return productMatch || categoryMatch || brandMatch;
    }

    private BigDecimal calcLineDiscount(Promotion p, BigDecimal lineSubtotal) {
        BigDecimal discount;
        if (p.getDiscountType() == Voucher.DiscountType.percent) {
            discount = lineSubtotal.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (p.getMaxDiscount() != null && discount.compareTo(p.getMaxDiscount()) > 0) {
                discount = p.getMaxDiscount();
            }
        } else if (p.getDiscountType() == Voucher.DiscountType.fixed_amount) {
            discount = p.getDiscountValue();
        } else {
            discount = BigDecimal.ZERO; // free_shipping khong anh huong subtotal
        }
        return discount.compareTo(lineSubtotal) > 0 ? lineSubtotal : discount;
    }

    @Data @AllArgsConstructor
    public static class LineItem {
        private Product product;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    @Data @AllArgsConstructor
    public static class PromotionCalcResult {
        private BigDecimal discountAmount;
        private BigDecimal cashBonus;
    }

    // ─── Admin CRUD ─────────────────────────────────────────────

    public List<PromotionResponse> getActiveForDisplay() {
        return getActivePromotions().stream().map(this::toResponse).toList();
    }

    public List<PromotionResponse> getAllAdmin() {
        return promotionRepo.findAll().stream().map(this::toResponse).toList();
    }

    public PromotionResponse create(Long adminUserId, PromotionRequest req) {
        Promotion promo = Promotion.builder()
            .promotionType(Promotion.PromotionType.valueOf(
                req.getPromotionType() != null ? req.getPromotionType() : "general"))
            .name(req.getName())
            .description(req.getDescription())
            .discountType(Voucher.DiscountType.valueOf(req.getDiscountType()))
            .discountValue(req.getDiscountValue())
            .minOrderValue(req.getMinOrderValue() != null ? req.getMinOrderValue() : BigDecimal.ZERO)
            .maxDiscount(req.getMaxDiscount())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .isActive(req.getIsActive() == null || req.getIsActive())
            .buildpcMinCpuDiscountPct(req.getBuildpcMinCpuDiscountPct())
            .buildpcMaxCpuDiscountPct(req.getBuildpcMaxCpuDiscountPct())
            .buildpcCashBonus(req.getBuildpcCashBonus())
            .buildpcMaxCashBonus(req.getBuildpcMaxCashBonus())
            .build();

        applyScope(promo, req);
        return toResponse(promotionRepo.save(promo));
    }

    public PromotionResponse update(Long id, PromotionRequest req) {
        Promotion promo = promotionRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Khuyến mãi"));

        if (req.getPromotionType() != null) promo.setPromotionType(Promotion.PromotionType.valueOf(req.getPromotionType()));
        promo.setName(req.getName());
        promo.setDescription(req.getDescription());
        if (req.getDiscountType() != null) promo.setDiscountType(Voucher.DiscountType.valueOf(req.getDiscountType()));
        promo.setDiscountValue(req.getDiscountValue());
        if (req.getMinOrderValue() != null) promo.setMinOrderValue(req.getMinOrderValue());
        promo.setMaxDiscount(req.getMaxDiscount());
        promo.setStartDate(req.getStartDate());
        promo.setEndDate(req.getEndDate());
        if (req.getIsActive() != null) promo.setIsActive(req.getIsActive());
        promo.setBuildpcMinCpuDiscountPct(req.getBuildpcMinCpuDiscountPct());
        promo.setBuildpcMaxCpuDiscountPct(req.getBuildpcMaxCpuDiscountPct());
        promo.setBuildpcCashBonus(req.getBuildpcCashBonus());
        promo.setBuildpcMaxCashBonus(req.getBuildpcMaxCashBonus());

        applyScope(promo, req);
        return toResponse(promotionRepo.save(promo));
    }

    public void delete(Long id) {
        if (!promotionRepo.existsById(id)) throw AppException.notFound("Khuyến mãi");
        promotionRepo.deleteById(id);
    }

    private void applyScope(Promotion promo, PromotionRequest req) {
        if (req.getProductIds() != null) promo.setProducts(productRepo.findAllById(req.getProductIds()));
        if (req.getCategoryIds() != null) promo.setCategories(categoryRepo.findAllById(req.getCategoryIds()));
        if (req.getBrandIds() != null) promo.setBrands(brandRepo.findAllById(req.getBrandIds()));
    }

    private PromotionResponse toResponse(Promotion p) {
        return PromotionResponse.builder()
            .id(p.getId()).promotionType(p.getPromotionType().name())
            .name(p.getName()).description(p.getDescription())
            .discountType(p.getDiscountType().name()).discountValue(p.getDiscountValue())
            .minOrderValue(p.getMinOrderValue()).maxDiscount(p.getMaxDiscount())
            .startDate(p.getStartDate()).endDate(p.getEndDate()).isActive(p.getIsActive())
            .buildpcMinCpuDiscountPct(p.getBuildpcMinCpuDiscountPct())
            .buildpcMaxCpuDiscountPct(p.getBuildpcMaxCpuDiscountPct())
            .buildpcCashBonus(p.getBuildpcCashBonus())
            .buildpcMaxCashBonus(p.getBuildpcMaxCashBonus())
            .productIds(p.getProducts().stream().map(Product::getId).toList())
            .categoryIds(p.getCategories().stream().map(Category::getId).toList())
            .brandIds(p.getBrands().stream().map(Brand::getId).toList())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
